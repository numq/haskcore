package io.github.numq.haskcore.service.text.buffer

import arrow.core.Either
import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.text.*
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

internal interface TextBuffer {
    val snapshot: StateFlow<TextSnapshot>

    val data: SharedFlow<TextEdit.Data>

    suspend fun changeLineEnding(lineEnding: TextLineEnding): Either<Throwable, Unit>

    suspend fun changeEncoding(encoding: TextEncoding): Either<Throwable, Unit>

    suspend fun insert(position: TextPosition, text: String): Either<Throwable, TextEdit.Data.Single?>

    suspend fun replace(range: TextRange, text: String): Either<Throwable, TextEdit.Data.Single?>

    suspend fun delete(range: TextRange): Either<Throwable, TextEdit.Data.Single?>

    suspend fun withBatch(block: suspend Raise<Throwable>.(TextBuffer) -> Unit): Either<Throwable, TextEdit.Data.Batch?>
}