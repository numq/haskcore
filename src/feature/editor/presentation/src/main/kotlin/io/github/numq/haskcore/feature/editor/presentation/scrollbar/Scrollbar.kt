package io.github.numq.haskcore.feature.editor.presentation.scrollbar

import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.feature.editor.presentation.measurements.Measurements
import io.github.numq.haskcore.platform.font.EditorFont
import kotlin.math.max

internal data class Scrollbar(val x: Float, val y: Float) {
    companion object {
        val ZERO = Scrollbar(x = 0f, y = 0f)
    }

    fun calculateScrollOffset(
        position: TextPosition, font: EditorFont, viewportWidth: Float, viewportHeight: Float, gutterWidth: Float
    ): Scrollbar {
        val (line, column) = position

        val caretTextOffset = column * font.charWidth

        val absoluteLineTop = line * font.lineHeight

        val absoluteLineBottom = absoluteLineTop + font.lineHeight

        val absoluteCaretX = Measurements.EDITOR_PADDING_START + caretTextOffset

        val verticalMargin = font.lineHeight

        val horizontalMargin = font.charWidth * 2

        val editorVisibleWidth = viewportWidth - gutterWidth

        val newX = when {
            absoluteCaretX < x + horizontalMargin -> max(
                0f, absoluteCaretX - horizontalMargin
            )

            absoluteCaretX > x + editorVisibleWidth - horizontalMargin -> absoluteCaretX - editorVisibleWidth + horizontalMargin

            else -> x
        }

        val newY = when {
            absoluteLineTop < y + verticalMargin -> max(
                0f, absoluteLineTop - verticalMargin
            )

            absoluteLineBottom > y + viewportHeight - verticalMargin -> absoluteLineBottom - viewportHeight + verticalMargin

            else -> y
        }

        return Scrollbar(x = newX, y = newY)
    }
}