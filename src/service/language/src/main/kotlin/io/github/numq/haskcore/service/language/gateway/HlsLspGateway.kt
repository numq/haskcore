package io.github.numq.haskcore.service.language.gateway

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.service.language.semantic.LegendType
import io.github.numq.haskcore.service.language.server.ServerProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.future.await
import org.eclipse.lsp4j.*
import java.net.URI

internal class HlsLspGateway(private val rootUri: String, private val serverProvider: ServerProvider) : LspGateway {
    private companion object {
        const val BUFFER_CAPACITY = 64
    }

    private val _semanticLegend = MutableStateFlow(emptyList<LegendType>())

    override val semanticLegend = _semanticLegend.asStateFlow()

    private val _events = MutableSharedFlow<LanguageEvent>(extraBufferCapacity = BUFFER_CAPACITY)

    override val events = _events.asSharedFlow()

    override fun handleEvent(event: LanguageEvent) {
        _events.tryEmit(event)
    }

    override suspend fun initialize() = Either.catch {
        val server = checkNotNull(serverProvider.server.value) { "Server not started" }

        val params = InitializeParams().apply {
            processId = ProcessHandle.current().pid().toInt()

            rootUri = this@HlsLspGateway.rootUri
            rootPath = rootUri?.let { URI(it).path }

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
                        tokenTypes = LegendType.entries.map(LegendType::value)
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

        _semanticLegend.value = result.capabilities?.semanticTokensProvider?.legend?.tokenTypes?.map { tokenType ->
            LegendType.fromValue(tokenType) ?: LegendType.VARIABLE
        } ?: emptyList()

        server.initialized(InitializedParams())

        result
    }

    override suspend fun didOpen(uri: String, text: String) = Either.catch {
        serverProvider.server.value?.textDocumentService?.didOpen(
            DidOpenTextDocumentParams(TextDocumentItem(uri, "haskell", 1, text))
        )
    }.map {}

    override suspend fun didChange(uri: String, version: Int, range: TextRange, newText: String) = Either.catch {
        val server = serverProvider.server.value ?: return@catch

        val changeEvent = TextDocumentContentChangeEvent().apply {
            this.range = Range(
                Position(range.start.line, range.start.column), Position(range.end.line, range.end.column)
            )

            this.text = newText
        }

        val params = DidChangeTextDocumentParams().apply {
            textDocument = VersionedTextDocumentIdentifier(uri, version)

            contentChanges = listOf(changeEvent)
        }

        server.textDocumentService.didChange(params)
    }.map {}

    override suspend fun requestSemanticTokens(uri: String) = Either.catch {
        val server = serverProvider.server.value ?: return@catch emptyList()

        val params = SemanticTokensParams(TextDocumentIdentifier(uri))

        server.textDocumentService?.semanticTokensFull(params)?.await()?.data ?: emptyList()
    }

    override suspend fun requestReferences(uri: String, position: TextPosition) = Either.catch {
        val server = serverProvider.server.value ?: return@catch emptyList()

        val params = ReferenceParams().apply {
            textDocument = TextDocumentIdentifier(uri)

            this.position = Position(position.line, position.column)

            context = ReferenceContext(true)
        }

        server.textDocumentService.references(params).await().map { location ->
            TextRange(
                start = TextPosition(line = location.range.start.line, column = location.range.start.character),
                end = TextPosition(line = location.range.end.line, column = location.range.end.character),
            )
        }
    }
}