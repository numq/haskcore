package io.github.numq.haskcore.feature.editor.core

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import io.github.numq.haskcore.common.core.text.TextEdit
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.core.text.TextSnapshot
import io.github.numq.haskcore.common.core.timestamp.Timestamp
import io.github.numq.haskcore.feature.editor.core.caret.CaretManager
import io.github.numq.haskcore.feature.editor.core.selection.SelectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.name

internal class LocalEditorService(
    private val scope: CoroutineScope,
    private val caretManager: CaretManager,
    private val selectionManager: SelectionManager,
    private val editorDataSource: EditorDataSource,
) : EditorService {
    override val caret = caretManager.caret

    override val selection = selectionManager.selection

    override val position = editorDataSource.editorData.map { editorData ->
        editorData.position.toEditorPosition()
    }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = EditorPosition())

    private val _activeLines = MutableStateFlow(IntRange.EMPTY)

    override val activeLines = _activeLines.asStateFlow()

    private val _collapsedLines = MutableStateFlow(emptySet<Int>())

    override val collapsedLines = _collapsedLines.asStateFlow()

    private val _collapsedRanges = MutableStateFlow(emptyList<IntRange>())

    override val collapsedRanges = _collapsedRanges.asStateFlow()

    private fun getActiveCollapsedRanges(): List<IntRange> {
        val collapsedStarts = _collapsedLines.value

        return _collapsedRanges.value.filter { ranges ->
            ranges.first in collapsedStarts
        }.map { range ->
            (range.first + 1)..range.last
        }
    }

    override suspend fun getParentPath(path: String) = Either.catch {
        withContext(Dispatchers.IO) {
            Path.of(path).parent.toString()
        }
    }

    override suspend fun getName(path: String) = Either.catch {
        withContext(Dispatchers.IO) {
            Path.of(path).name
        }
    }

    override suspend fun getLastModifiedTimestamp(path: String) = Either.catch {
        withContext(Dispatchers.IO) {
            Timestamp(nanoseconds = Path.of(path).getLastModifiedTime().to(TimeUnit.NANOSECONDS))
        }
    }

    override suspend fun saveEditorPosition(position: EditorPosition) = editorDataSource.update { editorData ->
        editorData.copy(position = position.toEditorPositionData())
    }.map { }

    override suspend fun updateActiveLines(start: Int, end: Int): Either<Throwable, Unit> {
        _activeLines.value = start..end

        return Unit.right()
    }

    override suspend fun updateCollapsedLines(lines: Set<Int>): Either<Throwable, Unit> {
        _collapsedLines.value = lines

        return Unit.right()
    }

    override suspend fun updateFoldingRegions(ranges: List<IntRange>): Either<Throwable, Unit> {
        _collapsedRanges.value = ranges

        return Unit.right()
    }

    override suspend fun handleEdit(snapshot: TextSnapshot, edit: TextEdit?) = either {
        selectionManager.clearSelection().bind()

        edit?.data?.let { data ->
            caretManager.handleTextEdit(snapshot = snapshot, data = data).bind()
        }

        Unit
    }

    override suspend fun moveCaret(
        snapshot: TextSnapshot, position: TextPosition,
    ) = caretManager.moveTo(snapshot = snapshot, position = position)

    override suspend fun moveCaretLeft(snapshot: TextSnapshot) = caretManager.moveLeft(
        snapshot = snapshot, collapsedRanges = getActiveCollapsedRanges()
    )

    override suspend fun moveCaretRight(snapshot: TextSnapshot) = caretManager.moveRight(
        snapshot = snapshot, collapsedRanges = getActiveCollapsedRanges()
    )

    override suspend fun moveCaretUp(snapshot: TextSnapshot) = caretManager.moveUp(
        snapshot = snapshot, collapsedRanges = getActiveCollapsedRanges()
    )

    override suspend fun moveCaretDown(snapshot: TextSnapshot) = caretManager.moveDown(
        snapshot = snapshot, collapsedRanges = getActiveCollapsedRanges()
    )

    override suspend fun startSelection(
        snapshot: TextSnapshot, position: TextPosition,
    ) = selectionManager.startSelection(position = position)

    override suspend fun extendSelection(
        snapshot: TextSnapshot, position: TextPosition,
    ) = selectionManager.extendSelection(position = position)

    override suspend fun selectAll(snapshot: TextSnapshot) = selectionManager.selectAll(snapshot = snapshot)

    override suspend fun clearSelection() = selectionManager.clearSelection()

    override fun close() {
        scope.cancel()
    }
}