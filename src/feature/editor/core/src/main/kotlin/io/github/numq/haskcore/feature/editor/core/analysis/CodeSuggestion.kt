package io.github.numq.haskcore.feature.editor.core.analysis

import io.github.numq.haskcore.core.text.TextRange

data class CodeSuggestion(
    val label: String,
    val kind: Kind,
    val insertText: String,
    val detail: String? = null,
    val documentation: String? = null,
    val range: TextRange? = null
) {
    enum class Kind {
        METHOD, FUNCTION, CONSTRUCTOR, FIELD, VARIABLE, CLASS, INTERFACE, MODULE, PROPERTY, UNIT, VALUE, ENUM, KEYWORD, SNIPPET
    }
}