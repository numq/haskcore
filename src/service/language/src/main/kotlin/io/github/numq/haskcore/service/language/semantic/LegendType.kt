package io.github.numq.haskcore.service.language.semantic

enum class LegendType(val value: String) {
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
        fun fromValue(value: String) = entries.find { legendType -> legendType.value == value }
    }
}