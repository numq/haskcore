package io.github.numq.haskcore.feature.editor.core.highlighting.token

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.service.text.syntax.SyntaxToken

internal interface TokenProvider {
    suspend fun getTokens(range: TextRange): Either<Throwable, List<SyntaxToken>>
}