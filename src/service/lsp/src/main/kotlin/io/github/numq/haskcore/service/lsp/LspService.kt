package io.github.numq.haskcore.service.lsp

import arrow.core.Either
import io.github.numq.haskcore.core.text.*
import io.github.numq.haskcore.service.lsp.completion.LspCompletion
import io.github.numq.haskcore.service.lsp.connection.LspConnection
import io.github.numq.haskcore.service.lsp.diagnostic.LspDiagnostic
import io.github.numq.haskcore.service.lsp.message.LspMessage
import io.github.numq.haskcore.service.lsp.reference.LspReference
import io.github.numq.haskcore.service.lsp.token.LspToken
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface LspService : AutoCloseable {
    val connection: StateFlow<LspConnection>

    val completions: StateFlow<List<LspCompletion>>

    val diagnostics: StateFlow<List<LspDiagnostic>>

    val references: StateFlow<List<LspReference>>

    val tokens: StateFlow<List<LspToken>>

    val messages: SharedFlow<LspMessage>

    suspend fun start(hlsPath: String): Either<Throwable, Unit>

    suspend fun openDocument(path: String, text: String): Either<Throwable, Unit>

    suspend fun applyEdit(path: String, revision: TextRevision, edit: TextEdit): Either<Throwable, Unit>

    suspend fun requestCompletions(path: String, position: TextPosition): Either<Throwable, Unit>

    suspend fun requestReferences(path: String, position: TextPosition): Either<Throwable, Unit>

    suspend fun requestTokens(path: String, snapshot: TextSnapshot, range: TextRange): Either<Throwable, Unit>
}