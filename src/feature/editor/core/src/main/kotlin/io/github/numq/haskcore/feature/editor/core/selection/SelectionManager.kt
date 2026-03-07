package io.github.numq.haskcore.feature.editor.core.selection

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.core.text.TextSnapshot
import kotlinx.coroutines.flow.StateFlow

internal interface SelectionManager : AutoCloseable {
    val selection: StateFlow<Selection>

    suspend fun startSelection(position: TextPosition): Either<Throwable, Unit>

    suspend fun extendSelection(position: TextPosition): Either<Throwable, Unit>

    suspend fun selectWordAt(snapshot: TextSnapshot, position: TextPosition): Either<Throwable, Unit>

    suspend fun selectLine(snapshot: TextSnapshot, line: Int): Either<Throwable, Unit>

    suspend fun selectRange(range: TextRange): Either<Throwable, Unit>

    suspend fun selectAll(snapshot: TextSnapshot): Either<Throwable, Unit>

    suspend fun clearSelection(): Either<Throwable, Unit>
}