package io.github.numq.haskcore.feature.editor.core.analysis

import io.github.numq.haskcore.core.text.TextRevision

data class Analysis(
    val revision: TextRevision,
    val issues: List<CodeIssue>,
    val suggestions: List<CodeSuggestion>
)