package io.github.numq.haskcore.service.text.syntax

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextRange

internal interface ScopeProvider {
    suspend fun getScopes(range: TextRange): Either<Throwable, List<SyntaxScope>>
}