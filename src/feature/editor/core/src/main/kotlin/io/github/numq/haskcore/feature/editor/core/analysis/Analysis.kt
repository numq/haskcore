package io.github.numq.haskcore.feature.editor.core.analysis

import io.github.numq.haskcore.common.core.text.TextRevision
import io.github.numq.haskcore.feature.editor.core.token.Token

data class Analysis(
    val revision: TextRevision,
    val documentation: CodeDocumentation?,
    val issues: List<CodeIssue>,
    val suggestions: List<CodeSuggestion>,
    val tokensPerLine: Map<Int, List<Token>>,
)