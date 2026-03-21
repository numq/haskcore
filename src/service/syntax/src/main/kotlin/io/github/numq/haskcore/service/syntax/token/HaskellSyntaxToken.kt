package io.github.numq.haskcore.service.syntax.token

import io.github.numq.haskcore.core.text.TextRange

internal data class HaskellSyntaxToken(val range: TextRange, val type: Type) {
    enum class Type {
        VARIABLE, VARIABLE_PARAMETER, VARIABLE_MEMBER, VARIABLE_ID, FUNCTION, FUNCTION_CALL, FUNCTION_INFIX, TYPE, TYPE_VARIABLE, CONSTRUCTOR, MODULE, KEYWORD, KEYWORD_IMPORT, KEYWORD_CONDITIONAL, KEYWORD_REPEAT, KEYWORD_DIRECTIVE, KEYWORD_EXCEPTION, KEYWORD_DEBUG, STRING, STRING_ESCAPE, NUMBER, NUMBER_FLOAT, CHARACTER, BOOLEAN, OPERATOR, PUNCTUATION_BRACKET, PUNCTUATION_DELIMITER, COMMENT, COMMENT_DOCUMENTATION, QUASI_QUOTE, DEFAULT;

        fun isMultilineAllowed() = when (this) {
            COMMENT, COMMENT_DOCUMENTATION, STRING -> true

            else -> false
        }
    }
}