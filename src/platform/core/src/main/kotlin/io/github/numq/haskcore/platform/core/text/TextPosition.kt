package io.github.numq.haskcore.platform.core.text

data class TextPosition(val line: Int, val column: Int) : Comparable<TextPosition> {
    companion object {
        val ZERO = TextPosition(line = 0, column = 0)
    }

    init {
        require(line >= 0) { "Line must be non-negative: $line" }

        require(column >= 0) { "Column must be non-negative: $column" }
    }

    override fun compareTo(other: TextPosition) = compareValuesBy(this, other, TextPosition::line, TextPosition::column)
}