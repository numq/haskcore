package io.github.numq.haskcore.service.text.occurrence

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.core.text.TextSnapshot
import kotlinx.coroutines.flow.Flow

internal interface OccurrenceProvider {
    suspend fun getLocalOccurrences(snapshot: TextSnapshot, position: TextPosition): Either<Throwable, List<Occurrence>>

    suspend fun getGlobalOccurrences(
        snapshot: TextSnapshot, position: TextPosition, references: List<TextRange>
    ): Either<Throwable, Flow<Occurrence>>
}