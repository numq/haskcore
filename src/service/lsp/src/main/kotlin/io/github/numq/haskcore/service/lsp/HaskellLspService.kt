package io.github.numq.haskcore.service.lsp

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.identity
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.core.text.TextRevision
import io.github.numq.haskcore.core.text.TextSnapshot
import io.github.numq.haskcore.service.lsp.completion.LspCompletion
import io.github.numq.haskcore.service.lsp.connection.LspConnectionInternal
import io.github.numq.haskcore.service.lsp.diagnostic.LspDiagnostic
import io.github.numq.haskcore.service.lsp.message.LspMessage
import io.github.numq.haskcore.service.lsp.reference.LspReference
import io.github.numq.haskcore.service.lsp.token.LspToken
import io.github.numq.haskcore.service.lsp.token.LspTokenLegend
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import java.io.File
import java.net.URI
import java.util.concurrent.CompletableFuture
import io.github.numq.haskcore.core.text.TextEdit as Edit

internal class HaskellLspService(
    private val projectPath: String, private val scope: CoroutineScope
) : LspService, LanguageClient {
    private companion object {
        const val BUFFER_CAPACITY = 64

        const val LANGUAGE_ID = "haskell"

        const val BASE_RECONNECT_DELAY_MS = 1000L

        const val MAX_RECONNECT_DELAY_MS = 30_000L
    }

    private var job: Job? = null

    private var _reconnectAttempts by atomic(0)

    private val _connection = MutableStateFlow<LspConnectionInternal>(LspConnectionInternal.Disconnected)

    override val connection = _connection.map(LspConnectionInternal::toLspConnection).stateIn(
        scope = scope, started = SharingStarted.Eagerly, initialValue = _connection.value.toLspConnection()
    )

    private val _completions = MutableStateFlow(emptyList<LspCompletion>())

    override val completions = _completions.asStateFlow()

    private val _references = MutableStateFlow(emptyList<LspReference>())

    override val references = _references.asStateFlow()

    private val _tokens = MutableStateFlow(emptyList<LspToken>())

    override val tokens = _tokens.asStateFlow()

    private val _diagnostics = MutableStateFlow<List<LspDiagnostic>>(emptyList())

    override val diagnostics = _diagnostics.asStateFlow()

    private val _messages = MutableSharedFlow<LspMessage>(extraBufferCapacity = BUFFER_CAPACITY)

    override val messages = _messages.asSharedFlow()

    private fun buildUri(path: String) = File(path).toURI()

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

    private suspend fun establishConnection(hlsPath: String): LspConnectionInternal.Connected {
        val processBuilder = ProcessBuilder(hlsPath, "--lsp").apply {
            directory(File(projectPath))

            redirectError(ProcessBuilder.Redirect.DISCARD)

            redirectOutput(ProcessBuilder.Redirect.DISCARD)
        }

        val process = processBuilder.start()

        val launcher = Launcher.createLauncher(
            this, LanguageServer::class.java, process.inputStream, process.outputStream
        )

        val future = launcher.startListening()

        val server = launcher.remoteProxy

        val params = InitializeParams().apply {
            processId = ProcessHandle.current().pid().toInt()

            val uri = buildUri(path = projectPath)

            rootUri = uri.toString()
            rootPath = uri.path

            capabilities = ClientCapabilities().apply {
                workspace = WorkspaceClientCapabilities().apply {
                    applyEdit = true
                    workspaceEdit = WorkspaceEditCapabilities().apply {
                        documentChanges = true
                    }
                }

                textDocument = TextDocumentClientCapabilities().apply {
                    synchronization = SynchronizationCapabilities().apply {
                        dynamicRegistration = true
                        willSave = true
                        didSave = true
                    }

                    semanticTokens = SemanticTokensCapabilities().apply {
                        dynamicRegistration = true
                        formats = listOf("relative")
                        requests = SemanticTokensClientCapabilitiesRequests(true)
                        tokenTypes = LspToken.Type.entries.map(LspToken.Type::value)
                        tokenModifiers = emptyList()
                    }

                    completion = CompletionCapabilities().apply {
                        completionItem = CompletionItemCapabilities().apply {
                            snippetSupport = true
                            documentationFormat = listOf(MarkupKind.MARKDOWN, MarkupKind.PLAINTEXT)
                        }
                    }
                }
            }
        }

        val result = server.initialize(params).await()

        val tokenLegend = LspTokenLegend(
            types = result.capabilities?.semanticTokensProvider?.legend?.tokenTypes?.map { tokenType ->
                LspToken.Type.fromValue(tokenType) ?: LspToken.Type.VARIABLE
            }.orEmpty()
        )

        server.initialized(InitializedParams())

        return LspConnectionInternal.Connected(
            process = process, future = future, server = server, tokenLegend = tokenLegend
        )
    }

    private fun stop() {
        job?.cancel()

        (_connection.value as? LspConnectionInternal.Connected)?.close()

        _connection.value = LspConnectionInternal.Disconnected
    }

    override fun logMessage(message: MessageParams) {
        _messages.tryEmit(message.toLspMessage())
    }

    override fun showMessage(params: MessageParams) = Unit

    override fun telemetryEvent(obj: Any) = Unit

    override fun showMessageRequest(
        showMessageRequestParams: ShowMessageRequestParams
    ): CompletableFuture<MessageActionItem?>? = CompletableFuture.completedFuture(null)

    override suspend fun start(hlsPath: String) = Either.catch {
        stop()

        job = scope.launch {
            while (isActive) {
                try {
                    _connection.value = LspConnectionInternal.Connecting

                    val connectionResult = establishConnection(hlsPath = hlsPath)

                    _connection.value = connectionResult

                    _reconnectAttempts = 0

                    runInterruptible(Dispatchers.IO) { connectionResult.future.get() }
                } catch (throwable: Throwable) {
                    _connection.value = LspConnectionInternal.Error(throwable = throwable)
                } finally {
                    val delayTime =
                        (BASE_RECONNECT_DELAY_MS * (_reconnectAttempts + 1)).coerceAtMost(MAX_RECONNECT_DELAY_MS)

                    _messages.tryEmit(LspMessage.Warning(content = "HLS crashed. Reconnecting in ${delayTime}ms..."))

                    delay(delayTime)

                    _reconnectAttempts += 1
                }
            }
        }
    }

    override suspend fun openDocument(path: String, text: String) = when (val currentConnection = _connection.value) {
        is LspConnectionInternal.Connected -> Either.catch {
            val uri = buildUri(path = path)

            currentConnection.server.textDocumentService?.didOpen(
                DidOpenTextDocumentParams(TextDocumentItem(uri.toString(), LANGUAGE_ID, 1, text))
            ) ?: Unit
        }

        else -> Unit.right()
    }

    override suspend fun applyEdit(
        path: String, revision: TextRevision, edit: Edit
    ) = when (val currentConnection = _connection.value) {
        is LspConnectionInternal.Connected -> Either.catch {
            val uri = buildUri(path = path)

            val changeEvents = when (val data = edit.data) {
                is Edit.Data.Single -> listOf(data.toTextDocumentContentChangeEvent())

                is Edit.Data.Batch -> data.singles.map(Edit.Data.Single::toTextDocumentContentChangeEvent)
            }

            val params = DidChangeTextDocumentParams().apply {
                textDocument = VersionedTextDocumentIdentifier(uri.toString(), revision.value.toInt())

                contentChanges = changeEvents
            }

            currentConnection.server.textDocumentService?.didChange(params)
        }.map {}

        else -> Unit.right()
    }

    override suspend fun requestCompletions(path: String, position: TextPosition) = Either.catch {
        _completions.value = when (val currentConnection = _connection.value) {
            is LspConnectionInternal.Connected -> {
                val uri = buildUri(path = path)

                val params = CompletionParams(
                    TextDocumentIdentifier(uri.toString()), Position(position.line, position.column)
                )

                currentConnection.server.textDocumentService?.completion(params)?.await()
                    ?.map(::identity, CompletionList::getItems)?.map(CompletionItem::toLspCompletion).orEmpty()
            }

            else -> emptyList()
        }
    }

    override suspend fun requestReferences(
        path: String, position: TextPosition
    ) = Either.catch {
        _references.value = when (val currentConnection = _connection.value) {
            is LspConnectionInternal.Connected -> {
                val uri = buildUri(path = path)

                val params = ReferenceParams().apply {
                    this.textDocument = TextDocumentIdentifier(uri.toString())

                    this.position = Position(position.line, position.column)

                    this.context = ReferenceContext(true)
                }

                currentConnection.server.textDocumentService?.references(params)?.await()?.map(Location::toTextRange)
                    ?.map(::LspReference).orEmpty()
            }

            else -> emptyList()
        }
    }

    override suspend fun requestTokens(path: String, snapshot: TextSnapshot, range: TextRange) = Either.catch {
        _tokens.value = when (val currentConnection = _connection.value) {
            is LspConnectionInternal.Connected -> {
                val uri = buildUri(path = path)

                val params = SemanticTokensParams(TextDocumentIdentifier(uri.toString()))

                val data =
                    currentConnection.server.textDocumentService?.semanticTokensFull(params)?.await()?.data.orEmpty()

                decodeSemanticTokens(data = data, legend = currentConnection.tokenLegend).map { semanticTokens ->
                    semanticTokens.filter { semanticToken -> range.contains(semanticToken.range) }
                }.getOrElse { throwable ->
                    throw throwable
                }
            }

            else -> emptyList()
        }
    }

    override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams) {
        val path = File(URI(diagnostics.uri)).absolutePath

        _diagnostics.tryEmit(diagnostics.diagnostics.map { diagnostic ->
            diagnostic.toLspDiagnostic(path = path)
        })
    }

    override fun close() {
        scope.cancel()

        _connection.value.close()
    }
}