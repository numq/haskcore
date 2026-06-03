package io.github.numq.haskcore.service.lsp

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.core.text.TextRange
import io.github.numq.haskcore.common.core.text.TextRevision
import io.github.numq.haskcore.common.core.text.TextSnapshot
import io.github.numq.haskcore.service.lsp.completion.LspCompletion
import io.github.numq.haskcore.service.lsp.connection.LspConnectionInternal
import io.github.numq.haskcore.service.lsp.diagnostic.LspDiagnostic
import io.github.numq.haskcore.service.lsp.hover.LspHover
import io.github.numq.haskcore.service.lsp.message.LspMessage
import io.github.numq.haskcore.service.lsp.reference.LspReference
import io.github.numq.haskcore.service.lsp.token.LspToken
import io.github.numq.haskcore.service.lsp.token.LspTokenLegend
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import java.io.File
import java.net.URI
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import io.github.numq.haskcore.common.core.text.TextEdit as Edit

internal class HaskellLspService(
    private val projectPath: String, private val scope: CoroutineScope,
) : LspService, LanguageClient {
    private companion object {
        const val BUFFER_CAPACITY = 64

        const val LANGUAGE_ID = "haskell"

        const val BASE_RECONNECT_DELAY_MS = 1000L

        const val MAX_RECONNECT_DELAY_MS = 30_000L

    }

    private var job: Job? = null

    private val _reconnectAttempts = atomic(0)

    private val _connection = MutableStateFlow<LspConnectionInternal>(LspConnectionInternal.Disconnected)

    override val connection = _connection.map(LspConnectionInternal::toLspConnection).stateIn(
        scope = scope, started = SharingStarted.Eagerly, initialValue = _connection.value.toLspConnection()
    )

    private val _hover = MutableStateFlow<LspHover?>(null)

    override val hover = _hover.asStateFlow()

    private val _completions = MutableStateFlow(emptyList<LspCompletion>())

    override val completions = _completions.asStateFlow()

    private val _references = MutableStateFlow(emptyList<LspReference>())

    override val references = _references.asStateFlow()

    private val _tokens = MutableStateFlow(emptyMap<String, List<LspToken>>())

    override val tokens = _tokens.asStateFlow()

    private val _diagnostics = MutableStateFlow(emptyMap<String, List<LspDiagnostic>>())

    override val diagnostics = _diagnostics.asStateFlow()

    private val _messages = MutableSharedFlow<LspMessage>(extraBufferCapacity = BUFFER_CAPACITY)

    override val messages = _messages.asSharedFlow()

    private suspend fun <T> CompletableFuture<T>.awaitWithoutServerCancel(): T =
        suspendCancellableCoroutine { continuation ->
            whenComplete { result, exception ->
                when (exception) {
                    null -> continuation.resume(result)

                    else -> continuation.resumeWithException(exception)
                }
            }
        }

    internal fun Hover.extractContentsString(): String {
        val contents = this.contents ?: return ""

        return when {
            contents.isRight -> contents.right.value.orEmpty()

            contents.isLeft -> contents.left.orEmpty().joinToString("\n") { element ->
                when {
                    element == null -> ""

                    element.isLeft -> element.left.orEmpty()

                    element.isRight -> element.right.value.orEmpty()

                    else -> ""
                }
            }

            else -> ""
        }
    }

    private fun normalizePath(path: String) = try {
        File(path).canonicalFile.absolutePath
    } catch (_: Throwable) {
        File(path).absolutePath
    }

    fun buildUri(path: String): String {
        val normalized = path.replace("\\", "/")

        return if (normalized.startsWith("/")) {
            "file://$normalized"
        } else {
            "file:///$normalized"
        }
    }

    private fun decodeSemanticTokens(data: List<Int>, legend: LspTokenLegend) = either {
        ensure(data.size % 5 == 0) {
            IllegalArgumentException("Invalid semantic tokens data size: ${data.size}")
        }

        val tokens = ArrayList<LspToken>(data.size / 5)

        var currentLine = 0

        var currentStartChar = 0

        for (i in data.indices step 5) {
            val deltaLine = data[i]

            val deltaStartChar = data[i + 1]

            val length = data[i + 2]

            val typeIndex = data[i + 3]

            val modifiers = data[i + 4]

            currentLine += deltaLine

            currentStartChar = when (deltaLine) {
                0 -> currentStartChar + deltaStartChar

                else -> deltaStartChar
            }

            tokens.add(
                LspToken(
                    range = TextRange(
                        start = TextPosition(currentLine, currentStartChar),
                        end = TextPosition(currentLine, currentStartChar + length)
                    ), type = legend.types.getOrElse(typeIndex) { LspToken.Type.VARIABLE }, modifiers = modifiers
                )
            )
        }

        tokens
    }

    private suspend fun establishConnection(hlsPath: String) = withContext(Dispatchers.IO) {
        val projectDir = File(projectPath)

        val process = ProcessBuilder(hlsPath, "--lsp").directory(projectDir).start()

        scope.launch(Dispatchers.IO) {
            process.errorStream.bufferedReader().use { reader ->
                reader.forEachLine { line ->
//                    println("HLS stderr: $line") // todo
                }
            }
        }

        val launcher = Launcher.createLauncher(
            this@HaskellLspService, LanguageServer::class.java, process.inputStream, process.outputStream
        )

        val future = launcher.startListening()

        val server = launcher.remoteProxy

        val params = InitializeParams().apply {
            processId = ProcessHandle.current().pid().toInt()

            val uri = buildUri(path = projectPath)

            rootUri = uri
            rootPath = projectDir.canonicalFile.absolutePath

            initializationOptions = mapOf(
                "haskell" to mapOf(
                    "checkParents" to "NeverCheck", "checkProject" to false, "plugin" to mapOf(
                        "semanticTokens" to mapOf(
                            "globalOn" to true, "config" to mapOf(
                                "documentSymbolProviderEnabled" to false
                            )
                        ), "moduleName" to mapOf(
                            "globalOn" to false
                        ), "pragmas" to mapOf(
                            "globalOn" to false
                        )
                    )
                )
            )

            workspaceFolders = listOf(
                WorkspaceFolder(uri, projectDir.name)
            )

            capabilities = ClientCapabilities().apply {
                general = GeneralClientCapabilities().apply {
                    positionEncodings = listOf(PositionEncodingKind.UTF8)
                }

                workspace = WorkspaceClientCapabilities().apply {
                    applyEdit = true
                    workspaceEdit = WorkspaceEditCapabilities().apply {
                        documentChanges = true
                    }
                    didChangeWatchedFiles = DidChangeWatchedFilesCapabilities().apply {
                        dynamicRegistration = false
                    }
                    workspaceFolders = true
                    configuration = true
                }

                textDocument = TextDocumentClientCapabilities().apply {
                    synchronization = SynchronizationCapabilities().apply {
                        dynamicRegistration = false
                        willSave = true
                        didSave = true
                    }

                    completion = CompletionCapabilities().apply {
                        dynamicRegistration = false
                        completionItem = CompletionItemCapabilities().apply {
                            snippetSupport = true
                            documentationFormat = listOf(MarkupKind.MARKDOWN, MarkupKind.PLAINTEXT)
                        }
                    }

                    semanticTokens = SemanticTokensCapabilities().apply {
                        dynamicRegistration = false
                        formats = listOf("relative")
                        requests = SemanticTokensClientCapabilitiesRequests().apply {
                            setFull(true)
                        }
                        tokenTypes = LspToken.Type.entries.map(LspToken.Type::value)
                        tokenModifiers = emptyList()
                    }

                    hover = HoverCapabilities().apply {
                        dynamicRegistration = false
                        contentFormat = listOf(MarkupKind.MARKDOWN, MarkupKind.PLAINTEXT)
                    }

                    definition = DefinitionCapabilities().apply {
                        dynamicRegistration = false
                    }
                }
            }
        }

        val result = server.initialize(params).awaitWithoutServerCancel()

        server.initialized(InitializedParams())

        val tokenLegend = LspTokenLegend(
            types = result.capabilities?.semanticTokensProvider?.legend?.tokenTypes?.map { tokenType ->
                LspToken.Type.fromValue(tokenType) ?: LspToken.Type.VARIABLE
            }.orEmpty()
        )

        LspConnectionInternal.Connected(
            process = process, future = future, server = server, tokenLegend = tokenLegend
        )
    }

    private fun startLifecycleMonitoring(hlsPath: String) {
        job?.cancel()

        job = scope.launch(Dispatchers.IO) {
            _connection.filterIsInstance<LspConnectionInternal.Connected>().collectLatest { connection ->
                try {
                    runInterruptible { connection.future.get() }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (throwable: Throwable) {
                    currentCoroutineContext().ensureActive()

                    println(throwable) // todo
                } finally {
                    if (_connection.value == connection) {
                        handleReconnect(hlsPath)
                    }
                }
            }
        }
    }

    private suspend fun handleReconnect(hlsPath: String) {
        val currentAttempt = _reconnectAttempts.incrementAndGet()

        val delayMs = (BASE_RECONNECT_DELAY_MS * (1 shl (currentAttempt - 1).coerceAtMost(30))).coerceAtMost(
            MAX_RECONNECT_DELAY_MS
        )

        _connection.value = LspConnectionInternal.Connecting

        delay(delayMs)

        start(hlsPath = hlsPath)
    }

    override fun telemetryEvent(obj: Any) = Unit

    override fun showMessage(params: MessageParams) = Unit

    override fun showMessageRequest(
        showMessageRequestParams: ShowMessageRequestParams,
    ): CompletableFuture<MessageActionItem?>? = CompletableFuture.completedFuture(null)

    override fun logMessage(message: MessageParams) {
        _messages.tryEmit(message.toLspMessage())
    }

    override fun registerCapability(params: RegistrationParams): CompletableFuture<Void> =
        CompletableFuture.completedFuture(null)

    override fun unregisterCapability(params: UnregistrationParams): CompletableFuture<Void> =
        CompletableFuture.completedFuture(null)

    override fun workspaceFolders(): CompletableFuture<List<WorkspaceFolder>> = CompletableFuture.completedFuture(
        listOf(WorkspaceFolder(buildUri(path = projectPath), File(projectPath).name))
    )

    override fun configuration(configurationParams: ConfigurationParams?): CompletableFuture<List<Any>> =
        CompletableFuture.completedFuture(emptyList())

    override fun applyEdit(params: ApplyWorkspaceEditParams): CompletableFuture<ApplyWorkspaceEditResponse> =
        CompletableFuture.completedFuture(ApplyWorkspaceEditResponse(true))

    override suspend fun start(hlsPath: String): Either<Throwable, Unit> {
        _connection.getAndUpdate { LspConnectionInternal.Connecting }.close()

        return Either.catch {
            val nextConnection = establishConnection(hlsPath = hlsPath)

            _connection.value = nextConnection

            _reconnectAttempts.value = 0

            startLifecycleMonitoring(hlsPath = hlsPath)
        }.onLeft { throwable ->
            _connection.value = LspConnectionInternal.Error(throwable = throwable)

            scope.launch { handleReconnect(hlsPath = hlsPath) }
        }
    }

    override suspend fun openDocument(path: String, text: String) = when (val currentConnection = _connection.value) {
        is LspConnectionInternal.Connected -> Either.catch {
            val uri = buildUri(path = path)

            currentConnection.server.textDocumentService?.didOpen(
                DidOpenTextDocumentParams(TextDocumentItem(uri, LANGUAGE_ID, 1, text))
            ) ?: Unit
        }

        else -> Unit.right()
    }

    override suspend fun applyEdit(
        path: String, revision: TextRevision, edit: Edit,
    ) = when (val currentConnection = _connection.value) {
        is LspConnectionInternal.Connected -> Either.catch {
            val service = checkNotNull(currentConnection.server.textDocumentService) {
                "Text document service is not available"
            }

            val uri = buildUri(path = path)

            val changeEvents = when (val data = edit.data) {
                is Edit.Data.Single -> listOf(data.toTextDocumentContentChangeEvent())

                is Edit.Data.Batch -> data.singles.map(Edit.Data.Single::toTextDocumentContentChangeEvent)
            }

            val params = DidChangeTextDocumentParams().apply {
                textDocument = VersionedTextDocumentIdentifier(uri, revision.value.toInt())

                contentChanges = changeEvents
            }

            service.didChange(params)
        }

        else -> Unit.right()
    }

    override suspend fun requestHover(path: String, position: TextPosition) = try {
        when (val currentConnection = _connection.value) {
            is LspConnectionInternal.Connected -> {
                val uri = buildUri(path = path)

                val params = HoverParams().apply {
                    textDocument = TextDocumentIdentifier(uri)
                    this.position = Position(position.line, position.column)
                }

                _hover.value = currentConnection.server.textDocumentService?.hover(params)?.awaitWithoutServerCancel()
                    ?.let { response ->
                        val contentsString = response.extractContentsString()

                        val textRange = response.range?.let { lspRange ->
                            TextRange(
                                start = TextPosition(line = lspRange.start.line, column = lspRange.start.character),
                                end = TextPosition(line = lspRange.end.line, column = lspRange.end.character)
                            )
                        }

                        LspHover(content = contentsString, range = textRange)
                    }
            }

            else -> _hover.value = null
        }

        Unit.right()
    } catch (exception: CancellationException) {
        throw exception
    } catch (throwable: Throwable) {
        _hover.value = null

        throwable.left()
    }

    override suspend fun dismissHover(path: String) = Either.catch {
        _hover.value = null
    }

    override suspend fun requestCompletions(path: String, position: TextPosition) = try {
        when (val currentConnection = _connection.value) {
            is LspConnectionInternal.Connected -> {
                val uri = buildUri(path = path)

                val context = CompletionContext(CompletionTriggerKind.Invoked)

                val params = CompletionParams().apply {
                    textDocument = TextDocumentIdentifier(uri)
                    this.position = Position(position.line, position.column)
                    this.context = context
                }

                val response =
                    currentConnection.server.textDocumentService?.completion(params)?.awaitWithoutServerCancel()

                val items = when {
                    response == null -> emptyList()

                    response.isLeft -> response.left

                    response.isRight -> response.right?.items.orEmpty()

                    else -> emptyList()
                }

                _completions.value = items.map(CompletionItem::toLspCompletion)
            }

            else -> _completions.value = emptyList()
        }

        Unit.right()
    } catch (exception: CancellationException) {
        throw exception
    } catch (throwable: Throwable) {
        _completions.value = emptyList()

        throwable.left()
    }

    override suspend fun requestReferences(path: String, position: TextPosition) = try {
        when (val currentConnection = _connection.value) {
            is LspConnectionInternal.Connected -> {
                val uri = buildUri(path = path)

                val params = ReferenceParams().apply {
                    this.textDocument = TextDocumentIdentifier(uri)
                    this.position = Position(position.line, position.column)
                    this.context = ReferenceContext(true)
                }

                val references =
                    currentConnection.server.textDocumentService?.references(params)?.awaitWithoutServerCancel()
                        ?.map(Location::toTextRange)?.map(::LspReference).orEmpty()

                _references.value = references
            }

            else -> _references.value = emptyList()
        }

        Unit.right()
    } catch (exception: CancellationException) {
        throw exception
    } catch (throwable: Throwable) {
        _references.value = emptyList()

        throwable.left()
    }

    override suspend fun requestTokens(
        path: String, snapshot: TextSnapshot, range: TextRange,
    ): Either<Throwable, Unit> {
        val normalizedPath = normalizePath(path = path)

        return try {
            when (val currentConnection = _connection.value) {
                is LspConnectionInternal.Connected -> {
                    val uri = buildUri(path = path)

                    val params = SemanticTokensParams(TextDocumentIdentifier(uri))

                    val response = currentConnection.server.textDocumentService?.semanticTokensFull(params)
                        ?.awaitWithoutServerCancel()

                    val data = response?.data.orEmpty()

                    val tokens = decodeSemanticTokens(
                        data = data, legend = currentConnection.tokenLegend
                    ).getOrElse { throwable ->
                        throw throwable
                    }

                    _tokens.update { currentMap -> currentMap + (normalizedPath to tokens) }
                }

                else -> _tokens.update { currentMap -> currentMap + (normalizedPath to emptyList()) }
            }

            Unit.right()
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Throwable) {
            _tokens.update { currentMap -> currentMap + (normalizedPath to emptyList()) }

            throwable.left()
        }
    }

    override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams) {
        scope.launch(Dispatchers.IO) {
            try {
                val path = File(URI(diagnostics.uri)).absolutePath

                val normalizedPath = normalizePath(path = path)

                val mappedDiagnostics = diagnostics.diagnostics.map(Diagnostic::toLspDiagnostic)

                _diagnostics.update { currentMap ->
                    currentMap + (normalizedPath to mappedDiagnostics)
                }
            } catch (throwable: Throwable) {
                println("Error parsing diagnostics: $throwable") // todo
            }
        }
    }

    override suspend fun closeDocument(path: String): Either<Nothing, Unit> {
        val normalizedPath = normalizePath(path = path)

        _diagnostics.update { diagnosticMap -> diagnosticMap - normalizedPath }

        _tokens.update { tokenMap -> tokenMap - normalizedPath }

        return Unit.right()
    }

    override suspend fun stop(): Either<Nothing, Unit> {
        job?.cancel()

        job = null

        _reconnectAttempts.value = 0

        _connection.getAndUpdate { LspConnectionInternal.Disconnected }.close()

        _hover.value = null

        _completions.value = emptyList()

        _references.value = emptyList()

        _diagnostics.update { emptyMap() }

        _tokens.update { emptyMap() }

        return Unit.right()
    }

    override fun close() {
        scope.cancel()

        _connection.value.close()
    }
}