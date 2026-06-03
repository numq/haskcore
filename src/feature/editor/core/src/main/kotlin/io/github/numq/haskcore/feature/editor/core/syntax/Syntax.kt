package io.github.numq.haskcore.feature.editor.core.syntax

import io.github.numq.haskcore.common.core.text.TextRevision
import io.github.numq.haskcore.feature.editor.core.token.Token

data class Syntax(
    val revision: TextRevision,
    val foldingRegions: List<FoldingRegion>,
    val occurrences: List<Occurrence>,
    val tokensPerLine: Map<Int, List<Token>>,
)