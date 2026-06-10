package io.github.numq.haskcore.feature.editor.core

import arrow.core.Either
import io.github.numq.haskcore.common.core.text.TextEdit
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.core.text.TextSnapshot
import io.github.numq.haskcore.common.core.timestamp.Timestamp
import io.github.numq.haskcore.feature.editor.core.caret.Caret
import io.github.numq.haskcore.feature.editor.core.selection.Selection
import kotlinx.coroutines.flow.StateFlow

interface EditorService : AutoCloseable {
    val caret: StateFlow<Caret>

    val selection: StateFlow<Selection>

    val position: StateFlow<EditorPosition>

    val activeLines: StateFlow<IntRange>

    suspend fun getParentPath(path: String): Either<Throwable, String>

    suspend fun getName(path: String): Either<Throwable, String>

    suspend fun getLastModifiedTimestamp(path: String): Either<Throwable, Timestamp>

    suspend fun saveEditorPosition(position: EditorPosition): Either<Throwable, Unit>

    suspend fun updateActiveLines(start: Int, end: Int): Either<Throwable, Unit>

    suspend fun handleEdit(snapshot: TextSnapshot, edit: TextEdit?): Either<Throwable, Unit>

    suspend fun moveCaret(snapshot: TextSnapshot, position: TextPosition): Either<Throwable, Unit>

    suspend fun moveCaretLeft(snapshot: TextSnapshot): Either<Throwable, Unit>

    suspend fun moveCaretRight(snapshot: TextSnapshot): Either<Throwable, Unit>

    suspend fun moveCaretUp(snapshot: TextSnapshot): Either<Throwable, Unit>

    suspend fun moveCaretDown(snapshot: TextSnapshot): Either<Throwable, Unit>

    suspend fun startSelection(snapshot: TextSnapshot, position: TextPosition): Either<Throwable, Unit>

    suspend fun extendSelection(snapshot: TextSnapshot, position: TextPosition): Either<Throwable, Unit>

    suspend fun selectAll(snapshot: TextSnapshot): Either<Throwable, Unit>

    suspend fun clearSelection(): Either<Throwable, Unit>
}