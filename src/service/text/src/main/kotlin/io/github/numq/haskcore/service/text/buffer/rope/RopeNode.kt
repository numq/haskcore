package io.github.numq.haskcore.service.text.buffer.rope

import kotlin.math.max

internal sealed interface RopeNode {
    enum class Color {
        RED, BLACK
    }

    val charCount: Int

    val byteCount: Int

    val lineBreakCount: Int

    val prefixLineLength: Int

    val suffixLineLength: Int

    val longestLineThroughSplit: Int

    val maxLineLength: Int

    val color: Color

    val height: Int

    val left: RopeNode

    val right: RopeNode

    data class Leaf(
        val content: String,
        override val byteCount: Int,
        override val lineBreakCount: Int,
        override val prefixLineLength: Int,
        override val suffixLineLength: Int,
        override val maxLineLength: Int,
        override val color: Color = Color.BLACK
    ) : RopeNode {
        override val charCount: Int get() = content.length

        override val longestLineThroughSplit = 0

        override val height: Int get() = 1

        override val left: RopeNode get() = Empty

        override val right: RopeNode get() = Empty

        val length: Int get() = content.length
    }

    data class Branch(
        override val left: RopeNode, override val right: RopeNode, override val color: Color = Color.BLACK
    ) : RopeNode {
        override val charCount = left.charCount + right.charCount

        override val byteCount = left.byteCount + right.byteCount

        override val lineBreakCount = left.lineBreakCount + right.lineBreakCount

        override val prefixLineLength = when (left.lineBreakCount) {
            0 -> left.charCount + right.prefixLineLength

            else -> left.prefixLineLength
        }

        override val suffixLineLength = when (right.lineBreakCount) {
            0 -> right.charCount + left.suffixLineLength

            else -> right.suffixLineLength
        }

        override val longestLineThroughSplit = maxOf(
            left.longestLineThroughSplit, right.longestLineThroughSplit, left.suffixLineLength + right.prefixLineLength
        )

        override val maxLineLength = maxOf(left.maxLineLength, right.maxLineLength, longestLineThroughSplit)

        override val height = 1 + max(left.height, right.height)
    }

    object Empty : RopeNode {
        override val charCount = 0

        override val byteCount = 0

        override val lineBreakCount = 0

        override val prefixLineLength = 0

        override val suffixLineLength = 0

        override val longestLineThroughSplit = 0

        override val maxLineLength = 0

        override val color = Color.BLACK

        override val height = 0

        override val left: RopeNode get() = this

        override val right: RopeNode get() = this
    }
}