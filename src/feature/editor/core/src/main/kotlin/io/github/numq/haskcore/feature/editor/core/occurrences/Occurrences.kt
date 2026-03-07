package io.github.numq.haskcore.feature.editor.core.occurrences

import io.github.numq.haskcore.feature.editor.core.highlighting.HighlightingToken

data class Occurrences(val tokens: List<HighlightingToken.Atom> = emptyList())