package io.github.numq.haskcore.feature.editor.core.analysis

import io.github.numq.haskcore.core.text.TextRange

sealed interface CodeIssue {
    val range: TextRange

    val message: String

    val source: String?
    val code: String?

    data class Unknown(
        override val range: TextRange,
        override val message: String,
        override val source: String? = null,
        override val code: String? = null
    ) : CodeIssue

    data class Error(
        override val range: TextRange,
        override val message: String,
        override val source: String? = null,
        override val code: String? = null
    ) : CodeIssue

    data class Warning(
        override val range: TextRange,
        override val message: String,
        override val source: String? = null,
        override val code: String? = null
    ) : CodeIssue

    data class Information(
        override val range: TextRange,
        override val message: String,
        override val source: String? = null,
        override val code: String? = null
    ) : CodeIssue

    data class Hint(
        override val range: TextRange,
        override val message: String,
        override val source: String? = null,
        override val code: String? = null
    ) : CodeIssue
}