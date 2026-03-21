package io.github.numq.haskcore.feature.editor.core.syntax

import io.github.numq.haskcore.core.text.TextRange

sealed interface Token {
    enum class Type {
        KEYWORD, KEYWORD_CONDITIONAL, KEYWORD_IMPORT, KEYWORD_REPEAT, KEYWORD_DIRECTIVE, KEYWORD_EXCEPTION, KEYWORD_DEBUG, TYPE, CONSTRUCTOR, BOOLEAN, FUNCTION, FUNCTION_CALL, VARIABLE, VARIABLE_PARAMETER, VARIABLE_MEMBER, OPERATOR, NUMBER, NUMBER_FLOAT, STRING, CHARACTER, STRING_SPECIAL_SYMBOL, COMMENT, COMMENT_DOCUMENTATION, PUNCTUATION_BRACKET, PUNCTUATION_DELIMITER, MODULE, SPELL, WILDCARD, LOCAL_DEFINITION, LOCAL_REFERENCE, UNKNOWN
    }

    val range: TextRange

    val type: Type

    data class Region(override val range: TextRange, override val type: Type) : Token

    data class Atom(override val range: TextRange, override val type: Type, val text: String) : Token
}