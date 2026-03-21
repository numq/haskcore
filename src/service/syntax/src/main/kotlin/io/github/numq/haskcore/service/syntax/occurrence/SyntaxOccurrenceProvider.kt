package io.github.numq.haskcore.service.syntax.occurrence

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextPosition

internal interface SyntaxOccurrenceProvider {
    suspend fun getSyntaxOccurrences(position: TextPosition): Either<Throwable, List<SyntaxOccurrence>>
}