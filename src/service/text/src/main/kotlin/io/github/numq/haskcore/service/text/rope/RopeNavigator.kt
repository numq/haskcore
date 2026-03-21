package io.github.numq.haskcore.service.text.rope

import io.github.numq.haskcore.core.text.TextLineEnding
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange
import kotlin.math.max

internal object RopeNavigator {
    const val LINE_BREAK_LENGTH = 1

    fun calculateLineLength(rope: Rope, line: Int) = when {
        rope.totalLines == 0 || line < 0 || line >= rope.totalLines -> 0

        else -> {
            require(line in 0 until rope.totalLines) { "Line $line out of bounds" }

            val lineStart = rope.getOffsetOfLine(lineIndex = line)

            when {
                lineStart > rope.totalChars -> 0

                else -> {
                    val lineEnd = when (line) {
                        rope.totalLines - 1 -> rope.totalChars

                        else -> {
                            val nextLineStart = rope.getOffsetOfLine(lineIndex = line + 1)

                            max(lineStart, nextLineStart - LINE_BREAK_LENGTH)
                        }
                    }

                    max(0, lineEnd - lineStart)
                }
            }
        }
    }

    fun calculateLastPosition(rope: Rope) = when (val totalLines = rope.totalLines) {
        0 -> TextPosition.ZERO

        else -> {
            val lastLineIndex = totalLines - 1

            val lastLineOffset = rope.getOffsetOfLine(lastLineIndex)

            val lastLineLength = rope.totalChars - lastLineOffset

            TextPosition(line = lastLineIndex, column = lastLineLength)
        }
    }

    fun getCharOffset(rope: Rope, position: TextPosition) = when (rope.totalChars) {
        0 -> 0

        else -> {
            require(
                isValidInsertPosition(rope = rope, position = position)
            ) { "Invalid position: $position" }

            when (position.line) {
                rope.totalLines -> rope.totalChars

                else -> rope.getOffsetOfLine(lineIndex = position.line) + position.column
            }
        }
    }

    fun getPosition(rope: Rope, charOffset: Int): TextPosition {
        require(charOffset in 0..rope.totalChars) {
            "Char offset $charOffset out of bounds (0..${rope.totalChars})"
        }

        return when (charOffset) {
            rope.totalChars -> calculateLastPosition(rope = rope)

            else -> {
                val (line, column) = rope.getPositionAtOffset(charOffset = charOffset)

                TextPosition(line = line, column = column)
            }
        }
    }

    fun getPreviousPosition(rope: Rope, position: TextPosition) = when {
        position.column > 0 -> TextPosition(line = position.line, column = position.column - 1)

        position.line > 0 -> {
            val prevLineLength = calculateLineLength(rope = rope, line = position.line - 1)

            TextPosition(line = position.line - 1, column = prevLineLength)
        }

        else -> TextPosition.ZERO
    }

    fun isValidPosition(rope: Rope, position: TextPosition) = when {
        position.line < 0 || position.column < 0 -> false

        position.line >= rope.totalLines -> false

        else -> position.column <= calculateLineLength(rope = rope, line = position.line)
    }

    fun isValidInsertPosition(rope: Rope, position: TextPosition) = when {
        position.line < 0 || position.column < 0 -> false

        position.line > rope.totalLines -> false

        position.line == rope.totalLines -> position.column == 0

        else -> position.column <= calculateLineLength(
            rope = rope, line = position.line
        )
    }

    fun isValidRange(rope: Rope, range: TextRange) = isValidInsertPosition(
        rope = rope, position = range.start
    ) && isValidInsertPosition(rope = rope, position = range.end)

    fun restoreLineEndings(text: String, textLineEnding: TextLineEnding) = when (textLineEnding) {
        TextLineEnding.LF -> text

        TextLineEnding.CRLF -> text.replace("\n", "\r\n")

        TextLineEnding.CR -> text.replace("\n", "\r")
    }

    fun getFullText(rope: Rope, textLineEnding: TextLineEnding) = restoreLineEndings(
        text = rope.getText(offset = 0, length = rope.totalChars), textLineEnding = textLineEnding
    )
}