package io.github.numq.haskcore.service.text.syntax

internal object SyntaxTokenMapper {
    private val captureMap = mapOf(
        "variable" to SyntaxTokenType.VARIABLE,
        "variable.parameter" to SyntaxTokenType.VARIABLE_PARAMETER,
        "variable.member" to SyntaxTokenType.VARIABLE_FIELD,
        "function" to SyntaxTokenType.FUNCTION,
        "function.call" to SyntaxTokenType.FUNCTION_CALL,
        "type" to SyntaxTokenType.TYPE,
        "constructor" to SyntaxTokenType.CONSTRUCTOR,
        "module" to SyntaxTokenType.MODULE,
        "keyword" to SyntaxTokenType.KEYWORD,
        "keyword.import" to SyntaxTokenType.KEYWORD_IMPORT,
        "keyword.conditional" to SyntaxTokenType.KEYWORD_CONDITIONAL,
        "keyword.repeat" to SyntaxTokenType.KEYWORD_REPEAT,
        "keyword.directive" to SyntaxTokenType.KEYWORD_DIRECTIVE,
        "keyword.exception" to SyntaxTokenType.KEYWORD_EXCEPTION,
        "keyword.debug" to SyntaxTokenType.KEYWORD_DEBUG,
        "operator" to SyntaxTokenType.OPERATOR,
        "punctuation.bracket" to SyntaxTokenType.PUNCTUATION_BRACKET,
        "punctuation.delimiter" to SyntaxTokenType.PUNCTUATION_DELIMITER,
        "string" to SyntaxTokenType.STRING,
        "number" to SyntaxTokenType.NUMBER,
        "number.float" to SyntaxTokenType.NUMBER_FLOAT,
        "char" to SyntaxTokenType.CHARACTER,
        "boolean" to SyntaxTokenType.BOOLEAN,
        "comment" to SyntaxTokenType.COMMENT,
        "comment.documentation" to SyntaxTokenType.COMMENT_DOCUMENTATION
    )

    fun parseSyntax(captureName: String) = when (val capture = captureMap[captureName]) {
        null -> {
            var current = captureName

            while (current.contains(".")) {
                current = current.substringBeforeLast(".")

                if (captureMap.contains(current)) {
                    break
                }
            }

            captureMap[current] ?: SyntaxTokenType.DEFAULT
        }

        else -> capture
    }
}