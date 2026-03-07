package io.github.numq.haskcore.feature.editor.core.highlighting.token

import io.github.numq.haskcore.service.text.syntax.SyntaxToken

internal object TokenComparator : Comparator<SyntaxToken> {
    override fun compare(o1: SyntaxToken, o2: SyntaxToken): Int {
        val lineCompare = o1.range.start.line.compareTo(o2.range.start.line)

        if (lineCompare != 0) return lineCompare

        return o1.range.start.column.compareTo(o2.range.start.column)
    }
}