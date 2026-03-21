package io.github.numq.haskcore.service.lsp.completion

import io.github.numq.haskcore.core.text.TextRange

data class LspCompletion(
    val label: String,
    val kind: Kind,
    val detail: String?,
    val documentation: String?,
    val insertText: String,
    val textEditRange: TextRange?,
    val sortText: String?,
    val filterText: String?
) {
    enum class Kind {
        METHOD, FUNCTION, CONSTRUCTOR, FIELD, VARIABLE, CLASS, INTERFACE, MODULE, PROPERTY, UNIT, VALUE, ENUM, KEYWORD, SNIPPET
    }
}