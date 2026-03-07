package io.github.numq.haskcore.feature.editor.core.highlighting

import io.github.numq.haskcore.core.text.TextRange

sealed interface HighlightingToken {
    val range: TextRange

    val type: HighlightingType

    data class Region(override val range: TextRange, override val type: HighlightingType) : HighlightingToken

    data class Atom(
        override val range: TextRange, override val type: HighlightingType, val text: String
    ) : HighlightingToken
}