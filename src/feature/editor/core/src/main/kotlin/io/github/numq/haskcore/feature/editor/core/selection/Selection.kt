package io.github.numq.haskcore.feature.editor.core.selection

import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange

data class Selection(val range: TextRange, val direction: SelectionDirection = SelectionDirection.FORWARD) {
    val anchor: TextPosition
        get() = if (direction == SelectionDirection.FORWARD) range.start else range.end

    val caret: TextPosition
        get() = if (direction == SelectionDirection.FORWARD) range.end else range.start

    companion object {
        val EMPTY = Selection(range = TextRange.EMPTY, direction = SelectionDirection.FORWARD)

        fun fromPositions(anchor: TextPosition, caret: TextPosition): Selection {
            val isForward = anchor <= caret

            val range = when {
                isForward -> TextRange(start = anchor, end = caret)

                else -> TextRange(start = caret, end = anchor)
            }

            return Selection(
                range = range, direction = if (isForward) SelectionDirection.FORWARD else SelectionDirection.BACKWARD
            )
        }
    }
}