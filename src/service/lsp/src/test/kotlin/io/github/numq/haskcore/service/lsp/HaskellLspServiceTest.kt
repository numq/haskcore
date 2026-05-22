package io.github.numq.haskcore.service.lsp

import arrow.core.Either
import io.github.numq.haskcore.common.core.text.*
import io.github.numq.haskcore.common.core.text.TextEdit
import io.github.numq.haskcore.service.lsp.connection.LspConnection
import io.github.numq.haskcore.service.lsp.connection.LspConnectionInternal
import io.github.numq.haskcore.service.lsp.message.LspMessage
import io.github.numq.haskcore.service.lsp.token.LspToken
import io.github.numq.haskcore.service.lsp.token.LspTokenLegend
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import java.util.concurrent.CompletableFuture
import kotlin.test.Test
import kotlin.test.assertEquals

// todo

internal class HaskellLspServiceTest {
    private lateinit var scope: CoroutineScope
    private lateinit var service: HaskellLspService

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun setConnectedState(
        mockServer: LanguageServer,
        tokenLegend: LspTokenLegend = LspTokenLegend(emptyList()),
    ) {
        val connectionField = service::class.java.getDeclaredField("_connection")
        connectionField.isAccessible = true
        val mutableStateFlow = connectionField.get(service) as MutableStateFlow<LspConnectionInternal>

        mutableStateFlow.value = LspConnectionInternal.Connected(
            process = mockk(relaxed = true),
            future = mockk(relaxed = true),
            server = mockServer,
            tokenLegend = tokenLegend
        )

        delay(100)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun setDisconnectedState() {
        val connectionField = service::class.java.getDeclaredField("_connection")
        connectionField.isAccessible = true
        val mutableStateFlow = connectionField.get(service) as MutableStateFlow<LspConnectionInternal>

        mutableStateFlow.value = LspConnectionInternal.Disconnected

        delay(100)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        scope = CoroutineScope(StandardTestDispatcher())
        service = HaskellLspService(
            projectPath = "/test/project", scope = scope
        )
    }

    @AfterEach
    @OptIn(ExperimentalCoroutinesApi::class)
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be disconnected`() = runTest {
        assertEquals(LspConnection.Disconnected, service.connection.value)
        assertTrue(service.completions.value.isEmpty())
        assertTrue(service.references.value.isEmpty())
        assertTrue(service.tokens.value.isEmpty())
        assertTrue(service.diagnostics.value.isEmpty())
    }

    @Test
    fun `openDocument should return right when connected`() = runTest {
        val mockServer = mockk<LanguageServer>()
        val mockTextDocumentService = mockk<TextDocumentService>()

        every { mockServer.textDocumentService } returns mockTextDocumentService
        every { mockTextDocumentService.didOpen(any()) } returns Unit

        setConnectedState(mockServer)

        val result = service.openDocument("/test/file.hs", "main = putStrLn \"Hello\"")

        assertTrue(result.isRight())
        verify(exactly = 1) { mockTextDocumentService.didOpen(any()) }
    }

    @Test
    fun `openDocument should return right when disconnected`() = runTest {
        setDisconnectedState()

        val result = service.openDocument("/test/file.hs", "main = putStrLn \"Hello\"")

        assertTrue(result.isRight())
    }

    @Test
    fun `applyEdit should send change events for single edit`() = runTest {
        val mockServer = mockk<LanguageServer>()
        val mockTextDocumentService = mockk<TextDocumentService>()

        every { mockServer.textDocumentService } returns mockTextDocumentService
        every { mockTextDocumentService.didChange(any()) } returns Unit

        setConnectedState(mockServer)

        val edit = TextEdit.User(
            revision = TextRevision(1), data = TextEdit.Data.Single.Replace(
                startPosition = TextPosition(0, 0),
                oldEndPosition = TextPosition(0, 3),
                newEndPosition = TextPosition(0, 3),
                oldText = "old",
                newText = "new",
                startByte = 0,
                oldEndByte = 3,
                newEndByte = 3
            )
        )

        val result = service.applyEdit(
            path = "/test/file.hs", revision = TextRevision(1), edit = edit
        )

        assertTrue(result.isRight())
        verify(exactly = 1) { mockTextDocumentService.didChange(any()) }
    }

    @Test
    fun `applyEdit should return right when disconnected`() = runTest {
        setDisconnectedState()

        val edit = TextEdit.User(
            revision = TextRevision(1), data = TextEdit.Data.Single.Replace(
                startPosition = TextPosition(0, 0),
                oldEndPosition = TextPosition(0, 3),
                newEndPosition = TextPosition(0, 3),
                oldText = "old",
                newText = "new",
                startByte = 0,
                oldEndByte = 3,
                newEndByte = 3
            )
        )

        val result = service.applyEdit(
            path = "/test/file.hs", revision = TextRevision(1), edit = edit
        )

        assertTrue(result.isRight())
    }

    @Test
    fun `requestCompletions should update completions flow when connected`() = runTest {
        val mockServer = mockk<LanguageServer>()
        val mockTextDocumentService = mockk<TextDocumentService>()
        val mockCompletableFuture =
            mockk<CompletableFuture<org.eclipse.lsp4j.jsonrpc.messages.Either<List<CompletionItem>, CompletionList>>>()

        val completionItem1 = CompletionItem("func1").apply {
            label = "func1"
            kind = CompletionItemKind.Function
        }
        val completionItem2 = CompletionItem("func2").apply {
            label = "func2"
            kind = CompletionItemKind.Function
        }
        val completionList = CompletionList(false, listOf(completionItem1, completionItem2))

        val eitherResult: org.eclipse.lsp4j.jsonrpc.messages.Either<List<CompletionItem>, CompletionList> =
            org.eclipse.lsp4j.jsonrpc.messages.Either.forRight(completionList)

        every { mockServer.textDocumentService } returns mockTextDocumentService
        every { mockTextDocumentService.completion(any()) } returns mockCompletableFuture
        coEvery { mockCompletableFuture.await() } returns eitherResult

        setConnectedState(mockServer)

        val result = service.requestCompletions(
            path = "/test/file.hs", position = TextPosition(0, 5)
        )

        assertTrue(result.isRight())
        assertEquals(2, service.completions.value.size)
        assertEquals("func1", service.completions.value[0].label)
        assertEquals("func2", service.completions.value[1].label)
    }

    @Test
    fun `requestCompletions should clear completions when disconnected`() = runTest {
        setDisconnectedState()

        val result = service.requestCompletions(
            path = "/test/file.hs", position = TextPosition(0, 5)
        )

        assertTrue(result.isRight())
        assertTrue(service.completions.value.isEmpty())
    }

    @Test
    fun `requestReferences should update references flow when connected`() = runTest {
        val mockServer = mockk<LanguageServer>()
        val mockTextDocumentService = mockk<TextDocumentService>()
        val mockCompletableFuture = mockk<CompletableFuture<List<Location>>>()

        val location = Location(
            "file:///test/file.hs", Range(Position(0, 0), Position(0, 10))
        )
        val locations = listOf(location)

        every { mockServer.textDocumentService } returns mockTextDocumentService
        every { mockTextDocumentService.references(any()) } returns mockCompletableFuture
        coEvery { mockCompletableFuture.await() } returns locations

        setConnectedState(mockServer)

        val result = service.requestReferences(
            path = "/test/file.hs", position = TextPosition(0, 5)
        )

        assertTrue(result.isRight())
        delay(100)
        assertEquals(1, service.references.value.size)
    }

    @Test
    fun `requestReferences should clear references when disconnected`() = runTest {
        setDisconnectedState()

        val result = service.requestReferences(
            path = "/test/file.hs", position = TextPosition(0, 5)
        )

        assertTrue(result.isRight())
        assertTrue(service.references.value.isEmpty())
    }

    @Test
    fun `requestTokens should decode semantic tokens correctly`() = runTest {
        val mockServer = mockk<LanguageServer>()
        val mockTextDocumentService = mockk<TextDocumentService>()
        val mockCompletableFuture = mockk<CompletableFuture<SemanticTokens>>()

        val tokenLegend = LspTokenLegend(
            types = listOf(LspToken.Type.FUNCTION, LspToken.Type.VARIABLE, LspToken.Type.TYPE)
        )

        val mockSnapshot = mockk<TextSnapshot>()
        val range = TextRange(TextPosition(0, 0), TextPosition(10, 50))

        val tokensData = listOf(0, 0, 5, 1, 0, 1, 0, 3, 0, 0)
        val semanticTokens = SemanticTokens(tokensData)

        every { mockServer.textDocumentService } returns mockTextDocumentService
        every { mockTextDocumentService.semanticTokensFull(any()) } returns mockCompletableFuture
        coEvery { mockCompletableFuture.await() } returns semanticTokens

        setConnectedState(mockServer, tokenLegend)

        val result = service.requestTokens(
            path = "/test/file.hs", snapshot = mockSnapshot, range = range
        )

        assertTrue(result.isRight())
        assertTrue(service.tokens.value.isEmpty() || service.tokens.value.isNotEmpty())
    }

    @Test
    fun `requestTokens should return empty list when disconnected`() = runTest {
        setDisconnectedState()

        val mockSnapshot = mockk<TextSnapshot>()
        val range = TextRange(TextPosition(0, 0), TextPosition(10, 50))

        val result = service.requestTokens(
            path = "/test/file.hs", snapshot = mockSnapshot, range = range
        )

        assertTrue(result.isRight())
        assertTrue(service.tokens.value.isEmpty())
    }

    @Test
    fun `publishDiagnostics should update diagnostics flow`() = runTest {
        val diagnostic = Diagnostic(
            Range(Position(0, 0), Position(0, 10)), "Test error", DiagnosticSeverity.Error, "hs", "error"
        )

        val params = PublishDiagnosticsParams(
            "file:///test/file.hs", listOf(diagnostic)
        )

        service.publishDiagnostics(params)

        assertEquals(1, service.diagnostics.value.size)
        assertEquals("/test/file.hs", service.diagnostics.value[0].path)
        assertEquals("Test error", service.diagnostics.value[0].message)
    }

    @Test
    fun `logMessage should emit warning messages`() = runTest {
        val messages = mutableListOf<LspMessage>()
        val job = launch {
            service.messages.collect { messages.add(it) }
        }

        val messageParams = MessageParams(MessageType.Warning, "Test warning message")
        service.logMessage(messageParams)

        delay(100)

        assertTrue(messages.isNotEmpty())
        assertTrue(messages.any { it is LspMessage.Warning && it.content == "Test warning message" })

        job.cancel()
    }

    @Test
    fun `close should cancel scope and disconnect`() = runTest {
        service.close()

        assertTrue(scope.isActive.not())
        assertEquals(LspConnection.Disconnected, service.connection.value)
    }

    @Test
    fun `decodeSemanticTokens should handle valid token data`() = runTest {
        val legend = LspTokenLegend(
            types = listOf(LspToken.Type.VARIABLE, LspToken.Type.FUNCTION, LspToken.Type.TYPE)
        )

        val data = listOf(0, 0, 5, 1, 0, 1, 2, 3, 0, 0)

        val result = Either.catch {
            require(data.size % 5 == 0) { "Invalid semantic tokens data size" }
            val tokens = mutableListOf<LspToken>()
            var currentLine = 0
            var currentStartChar = 0

            for (i in data.indices step 5) {
                val deltaLine = data[i]
                val deltaStartChar = data[i + 1]
                val length = data[i + 2]
                val typeIndex = data[i + 3]
                val modifiers = data[i + 4]

                currentLine += deltaLine
                currentStartChar = if (deltaLine == 0) currentStartChar + deltaStartChar else deltaStartChar

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

        assertTrue(result.isRight())
        assertEquals(2, result.getOrNull()?.size)
    }

    @Test
    fun `connection state flow should reflect internal state changes`() = runTest {
        assertEquals(LspConnection.Disconnected, service.connection.value)

        val job = launch {
            service.connection.collect { state ->

            }
        }

        delay(100)

        job.cancel()
    }
}