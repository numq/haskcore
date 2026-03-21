package io.github.numq.haskcore.service.text.snapshot

import io.github.numq.haskcore.core.text.*
import io.github.numq.haskcore.service.text.rope.Rope
import io.github.numq.haskcore.service.text.rope.RopeNavigator
import java.nio.charset.Charset
import kotlin.math.max

internal class ImmutableTextSnapshot(
    private val rope: Rope,
    override val revision: TextRevision,
    override val charset: Charset,
    override val textLineEnding: TextLineEnding,
) : TextSnapshot {
    override val lines = rope.totalLines

    override val maxLineLength = rope.maxLineLength

    override val lastPosition get() = RopeNavigator.calculateLastPosition(rope = rope)

    override val text get() = RopeNavigator.getFullText(rope = rope, textLineEnding = textLineEnding)

    override fun isValidPosition(position: TextPosition) = when {
        position.line < 0 || position.column < 0 -> false

        position.line >= rope.totalLines -> false

        else -> position.column <= RopeNavigator.calculateLineLength(rope = rope, line = position.line)
    }

    override fun getLineText(line: Int): String {
        require(line in 0 until rope.totalLines) { "Line $line out of bounds (0..${rope.totalLines - 1})" }

        val lineStart = rope.getOffsetOfLine(lineIndex = line)

        val lineEnd = when (line) {
            rope.totalLines - 1 -> rope.totalChars

            else -> {
                val nextLineStart = rope.getOffsetOfLine(lineIndex = line + 1)

                max(lineStart, nextLineStart - RopeNavigator.LINE_BREAK_LENGTH)
            }
        }

        val text = rope.getText(offset = lineStart, length = lineEnd - lineStart)

        return RopeNavigator.restoreLineEndings(text = text, textLineEnding = textLineEnding)
    }

    override fun getLineLength(line: Int) = RopeNavigator.calculateLineLength(rope = rope, line = line)

    override fun getTextInRange(range: TextRange): String {
        require(RopeNavigator.isValidRange(rope = rope, range = range)) { "Invalid text range: $range" }

        return when {
            range.isEmpty -> ""

            else -> {
                val startOffset = RopeNavigator.getCharOffset(rope = rope, position = range.start)

                val endOffset = RopeNavigator.getCharOffset(rope = rope, position = range.end)

                val length = endOffset - startOffset

                val text = rope.getText(offset = startOffset, length = length)

                RopeNavigator.restoreLineEndings(text = text, textLineEnding = textLineEnding)
            }
        }
    }

    override fun getBytePosition(position: TextPosition) = when {
        !RopeNavigator.isValidPosition(rope = rope, position = position) -> null

        else -> {
            val charOffset = RopeNavigator.getCharOffset(rope = rope, position = position)

            rope.getByteOffset(charOffset = charOffset)
        }
    }

    override fun getTextPosition(bytePosition: Int): TextPosition? {
        return when {
            bytePosition < 0 || bytePosition > rope.totalBytes -> null

            bytePosition == 0 || rope.totalChars == 0 -> TextPosition.ZERO

            bytePosition >= rope.totalBytes -> RopeNavigator.calculateLastPosition(rope = rope)

            else -> {
                var low = 0

                var high = rope.totalChars - 1

                var result = 0

                while (low <= high) {
                    val mid = (low + high) ushr 1

                    val midByteOffset = rope.getByteOffset(charOffset = mid)

                    when {
                        midByteOffset < bytePosition -> {
                            result = mid

                            low = mid + 1
                        }

                        midByteOffset > bytePosition -> high = mid - 1

                        else -> return RopeNavigator.getPosition(rope = rope, charOffset = mid)
                    }
                }

                val currentByteOffset = rope.getByteOffset(charOffset = result)

                if (currentByteOffset <= bytePosition) {
                    return RopeNavigator.getPosition(rope = rope, charOffset = result)
                }

                if (result > 0) {
                    var i = result - 1

                    while (i >= 0) {
                        val offset = rope.getByteOffset(charOffset = i)

                        if (offset <= bytePosition) {
                            return RopeNavigator.getPosition(rope = rope, charOffset = i)
                        }

                        i--
                    }
                }

                TextPosition.ZERO
            }
        }
    }
}