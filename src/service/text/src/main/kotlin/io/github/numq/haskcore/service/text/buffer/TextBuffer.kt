package io.github.numq.haskcore.service.text.buffer

import arrow.core.Either
import arrow.core.raise.Raise
import io.github.numq.haskcore.core.text.*
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.charset.Charset

internal interface TextBuffer {
    val data: SharedFlow<TextEdit.Data>

    val snapshot: StateFlow<TextSnapshot>

    suspend fun changeLineEnding(lineEnding: LineEnding): Either<Throwable, Unit>

    suspend fun changeCharset(charset: Charset): Either<Throwable, Unit>

    suspend fun insert(position: TextPosition, text: String): Either<Throwable, TextEdit.Data.Single?>

    suspend fun replace(range: TextRange, text: String): Either<Throwable, TextEdit.Data.Single?>

    suspend fun delete(range: TextRange): Either<Throwable, TextEdit.Data.Single?>

    suspend fun withBatch(block: suspend Raise<Throwable>.(TextBuffer) -> Unit): Either<Throwable, TextEdit.Data.Batch?>
}