package io.github.numq.haskcore.service.lsp.token

import io.github.numq.haskcore.core.text.TextRange

data class LspToken(val range: TextRange, val type: Type, val modifiers: Int = 0) {
    enum class Type(val value: String) {
        NAMESPACE("namespace"), TYPE("type"), CLASS("class"), ENUM("enum"), INTERFACE("interface"), STRUCT("struct"), TYPE_PARAMETER(
            "typeParameter"
        ),
        PARAMETER("parameter"), VARIABLE("variable"), PROPERTY("property"), ENUM_MEMBER("enumMember"), EVENT("event"), FUNCTION(
            "function"
        ),
        METHOD("method"), MACRO("macro"), KEYWORD("keyword"), MODIFIER("modifier"), COMMENT("comment"), STRING("string"), NUMBER(
            "number"
        ),
        REGEXP("regexp"), OPERATOR("operator");

        companion object {
            fun fromValue(value: String) = entries.find { type -> type.value == value }
        }
    }
}