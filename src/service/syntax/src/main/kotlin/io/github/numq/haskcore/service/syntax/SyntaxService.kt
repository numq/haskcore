package io.github.numq.haskcore.service.syntax

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextEdit
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.core.text.TextSnapshot
import kotlinx.coroutines.flow.StateFlow

interface SyntaxService : AutoCloseable {
    val syntax: StateFlow<Syntax?>

    suspend fun fullParse(snapshot: TextSnapshot): Either<Throwable, Unit>

    suspend fun applyChange(
        snapshot: TextSnapshot, data: TextEdit.Data, range: TextRange, position: TextPosition
    ): Either<Throwable, Unit>

    suspend fun parseFoldingRegions(range: TextRange): Either<Throwable, Unit>

    suspend fun parseOccurrences(position: TextPosition): Either<Throwable, Unit>

    suspend fun parseTokensPerLine(snapshot: TextSnapshot, range: TextRange): Either<Throwable, Unit>
}