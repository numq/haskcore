package io.github.numq.haskcore.feature.editor.core.highlighting

data class Highlighting(
    val scopes: List<HighlightingScope> = emptyList(), val tokensPerLine: Map<Int, List<HighlightingToken>> = emptyMap()
)