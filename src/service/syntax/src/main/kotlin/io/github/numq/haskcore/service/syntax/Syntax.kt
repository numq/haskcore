package io.github.numq.haskcore.service.syntax

import io.github.numq.haskcore.core.text.TextRevision
import io.github.numq.haskcore.service.syntax.folding.SyntaxFoldingRegion
import io.github.numq.haskcore.service.syntax.occurrence.SyntaxOccurrence
import io.github.numq.haskcore.service.syntax.token.SyntaxToken

data class Syntax(
    val revision: TextRevision,
    val text: String,
    val lineLengths: Map<Int, Int> = emptyMap(),
    val foldingRegions: List<SyntaxFoldingRegion> = emptyList(),
    val occurrences: List<SyntaxOccurrence> = emptyList(),
    val tokensPerLine: Map<Int, List<SyntaxToken>> = emptyMap(),
)