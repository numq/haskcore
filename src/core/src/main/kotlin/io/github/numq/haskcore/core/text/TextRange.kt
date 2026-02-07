package io.github.numq.haskcore.core.text

data class TextRange(val start: TextPosition, val end: TextPosition) {
    companion object {
        val EMPTY = TextRange(TextPosition.ZERO, TextPosition.ZERO)

        fun fromPositions(p1: TextPosition, p2: TextPosition) = when {
            p1 <= p2 -> TextRange(start = p1, end = p2)

            else -> TextRange(start = p2, end = p1)
        }
    }

    init {
        require(start <= end) { "Start must be <= end: start=$start, end=$end" }
    }

    val isEmpty: Boolean get() = start == end

    val isNotEmpty: Boolean get() = start != end

    val isSingleLine: Boolean get() = start.line == end.line

    val isMultiLine: Boolean get() = start.line != end.line

    fun contains(position: TextPosition): Boolean {
        if (position.line < start.line || position.line > end.line) return false

        return when {
            isSingleLine -> position.column in start.column..end.column

            position.line == start.line -> position.column >= start.column

            position.line == end.line -> position.column <= end.column

            else -> true
        }
    }

    fun contains(other: TextRange) = start <= other.start && end >= other.end

    fun intersects(other: TextRange) = start < other.end && other.start < end
}