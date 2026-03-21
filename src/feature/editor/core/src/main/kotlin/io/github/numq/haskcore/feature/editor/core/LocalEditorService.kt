package io.github.numq.haskcore.feature.editor.core

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import io.github.numq.haskcore.core.text.TextEdit
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextSnapshot
import io.github.numq.haskcore.feature.editor.core.caret.CaretManager
import io.github.numq.haskcore.feature.editor.core.selection.SelectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(FlowPreview::class)
internal class LocalEditorService(
    private val scope: CoroutineScope,
    private val caretManager: CaretManager,
    private val selectionManager: SelectionManager
) : EditorService {
    override val caret = caretManager.caret

    override val selection = selectionManager.selection

    private val _activeLines = MutableStateFlow(IntRange.EMPTY)

    override val activeLines = _activeLines.asStateFlow()

    override suspend fun updateActiveLines(start: Int, end: Int): Either<Throwable, Unit> {
        _activeLines.value = start..end

        return Unit.right()
    }

    override suspend fun handleEdit(snapshot: TextSnapshot, edit: TextEdit?) = either {
        selectionManager.clearSelection().bind()

        edit?.data?.let { data ->
            caretManager.handleTextEdit(snapshot = snapshot, data = data).bind()
        }

        Unit
    }

    override suspend fun moveCaret(snapshot: TextSnapshot, position: TextPosition) = caretManager.moveTo(
        snapshot = snapshot, position = position
    )

    override suspend fun moveCaretLeft(snapshot: TextSnapshot) = caretManager.moveLeft(snapshot = snapshot)

    override suspend fun moveCaretRight(snapshot: TextSnapshot) = caretManager.moveRight(snapshot = snapshot)

    override suspend fun moveCaretUp(snapshot: TextSnapshot) = caretManager.moveUp(snapshot = snapshot)

    override suspend fun moveCaretDown(snapshot: TextSnapshot) = caretManager.moveDown(snapshot = snapshot)

    override suspend fun startSelection(snapshot: TextSnapshot, position: TextPosition) =
        selectionManager.startSelection(position = position)

    override suspend fun extendSelection(snapshot: TextSnapshot, position: TextPosition) =
        selectionManager.extendSelection(position = position)

    override suspend fun selectAll(snapshot: TextSnapshot) = selectionManager.selectAll(snapshot = snapshot)

    override suspend fun clearSelection() = selectionManager.clearSelection()

    override fun close() {
        scope.cancel()
    }
}