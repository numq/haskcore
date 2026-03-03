package io.github.numq.haskcore.service.language.gateway

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.service.language.semantic.LegendType
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.lsp4j.InitializeResult

internal interface LspGateway {
    val semanticLegend: StateFlow<List<LegendType>>

    val events: SharedFlow<LanguageEvent>

    fun handleEvent(event: LanguageEvent)

    suspend fun initialize(): Either<Throwable, InitializeResult>

    suspend fun didOpen(uri: String, text: String): Either<Throwable, Unit>

    suspend fun didChange(uri: String, version: Int, range: TextRange, newText: String): Either<Throwable, Unit>

    suspend fun requestSemanticTokens(uri: String): Either<Throwable, List<Int>>

    suspend fun requestReferences(uri: String, position: TextPosition): Either<Throwable, List<TextRange>>
}