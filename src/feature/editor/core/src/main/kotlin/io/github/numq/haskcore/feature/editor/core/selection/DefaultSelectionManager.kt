package io.github.numq.haskcore.feature.editor.core.selection

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.core.text.TextSnapshot
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class DefaultSelectionManager(private val scope: CoroutineScope) : SelectionManager {
    private val _anchorPosition = atomic<TextPosition?>(null)

    private val _selection = MutableStateFlow(Selection.EMPTY)

    override val selection = _selection.asStateFlow()

    private fun isHaskellIdentifier(char: Char) = char.isLetterOrDigit() || char == '_' || char == '\''

    private fun isHaskellOperator(char: Char) = ":!#$%&*+./<=>?@\\^|-~".contains(char)

    private fun updateSelection(anchor: TextPosition, caret: TextPosition): Either<Throwable, Unit> = either {
        val newSelection = Selection.fromPositions(anchor = anchor, caret = caret)

        _selection.value = newSelection
    }

    private fun resetSelection() {
        val currentSelection = _selection.value

        if (!currentSelection.range.isEmpty) {
            _selection.value = Selection.EMPTY

            _anchorPosition.value = null
        }
    }

    override suspend fun startSelection(position: TextPosition) = either {
        _anchorPosition.value = position

        updateSelection(anchor = position, caret = position).bind()
    }

    override suspend fun extendSelection(position: TextPosition) = either {
        val anchor = _anchorPosition.value ?: position

        if (_anchorPosition.value == null) {
            _anchorPosition.value = anchor
        }

        updateSelection(anchor = anchor, caret = position).bind()
    }

    override suspend fun selectWordAt(snapshot: TextSnapshot, position: TextPosition) = either {
        val lineText = snapshot.getLineText(line = position.line)

        if (lineText.isEmpty()) return@either

        val col = position.column.coerceIn(0, lineText.length - 1)

        val firstChar = lineText[col]

        val predicate: (Char) -> Boolean = when {
            isHaskellIdentifier(firstChar) -> ::isHaskellIdentifier

            isHaskellOperator(firstChar) -> ::isHaskellOperator

            else -> { _ -> false }
        }

        if (!isHaskellIdentifier(firstChar) && !isHaskellOperator(firstChar)) {
            startSelection(position = position).bind()

            return@either
        }

        var startCol = col

        while (startCol > 0 && predicate(lineText[startCol - 1])) {
            startCol--
        }

        var endCol = col

        while (endCol < lineText.length && predicate(lineText[endCol])) {
            endCol++
        }

        val start = TextPosition(line = position.line, column = startCol)

        val end = TextPosition(line = position.line, column = endCol)

        _anchorPosition.value = start

        updateSelection(anchor = start, caret = end).bind()
    }

    override suspend fun selectLine(snapshot: TextSnapshot, line: Int) = either {
        val totalLines = snapshot.lines

        if (line < 0 || line >= totalLines) return@either

        val start = TextPosition(line = line, column = 0)

        val lineLength = snapshot.getLineLength(line = line)

        val end = TextPosition(line = line, column = lineLength)

        _anchorPosition.value = start

        updateSelection(anchor = start, caret = end).bind()
    }

    override suspend fun selectRange(range: TextRange): Either<Throwable, Unit> = either {
        val isForward = range.start <= range.end

        val anchor = when {
            isForward -> range.start

            else -> range.end
        }

        _selection.value = Selection(
            range = when {
                isForward -> range

                else -> TextRange(start = range.end, end = range.start)
            }, direction = when {
                isForward -> SelectionDirection.FORWARD

                else -> SelectionDirection.BACKWARD
            }
        )

        _anchorPosition.value = anchor
    }

    override suspend fun selectAll(snapshot: TextSnapshot) = either {
        val start = TextPosition.ZERO

        val end = snapshot.lastPosition

        _anchorPosition.value = start

        updateSelection(anchor = start, caret = end).bind()
    }

    override suspend fun clearSelection() = resetSelection().right()

    override fun close() {
        scope.cancel()
    }
}