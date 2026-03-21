package io.github.numq.haskcore.feature.editor.core.syntax

import io.github.numq.haskcore.core.text.TextRange

sealed interface FoldingRegion {
    val range: TextRange

    data class Expanded(override val range: TextRange) : FoldingRegion

    data class Collapsed(override val range: TextRange) : FoldingRegion
}