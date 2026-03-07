package io.github.numq.haskcore.feature.editor.core.caret

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextEdit
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextSnapshot
import kotlinx.coroutines.flow.StateFlow

internal interface CaretManager : AutoCloseable {
    val caret: StateFlow<Caret>

    suspend fun handleTextEdit(snapshot: TextSnapshot, data: TextEdit.Data): Either<Throwable, Unit>

    suspend fun moveTo(snapshot: TextSnapshot, position: TextPosition): Either<Throwable, Unit>

    suspend fun moveLeft(snapshot: TextSnapshot): Either<Throwable, Unit>

    suspend fun moveRight(snapshot: TextSnapshot): Either<Throwable, Unit>

    suspend fun moveUp(snapshot: TextSnapshot): Either<Throwable, Unit>

    suspend fun moveDown(snapshot: TextSnapshot): Either<Throwable, Unit>
}