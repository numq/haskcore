package io.github.numq.haskcore.service.text.syntax

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextRange

internal interface SyntaxTokenProvider {
    suspend fun getSyntaxTokens(range: TextRange): Either<Throwable, List<SyntaxToken>>
}