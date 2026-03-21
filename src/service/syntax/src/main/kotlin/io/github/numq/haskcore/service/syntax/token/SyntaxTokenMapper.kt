package io.github.numq.haskcore.service.syntax.token

internal object SyntaxTokenMapper {
    private val captureMap = mapOf(
        "variable" to HaskellSyntaxToken.Type.VARIABLE,
        "variable.parameter" to HaskellSyntaxToken.Type.VARIABLE_PARAMETER,
        "variable.member" to HaskellSyntaxToken.Type.VARIABLE_MEMBER,
        "function" to HaskellSyntaxToken.Type.FUNCTION,
        "function.call" to HaskellSyntaxToken.Type.FUNCTION_CALL,
        "type" to HaskellSyntaxToken.Type.TYPE,
        "constructor" to HaskellSyntaxToken.Type.CONSTRUCTOR,
        "module" to HaskellSyntaxToken.Type.MODULE,
        "keyword" to HaskellSyntaxToken.Type.KEYWORD,
        "keyword.import" to HaskellSyntaxToken.Type.KEYWORD_IMPORT,
        "keyword.conditional" to HaskellSyntaxToken.Type.KEYWORD_CONDITIONAL,
        "keyword.repeat" to HaskellSyntaxToken.Type.KEYWORD_REPEAT,
        "keyword.directive" to HaskellSyntaxToken.Type.KEYWORD_DIRECTIVE,
        "keyword.exception" to HaskellSyntaxToken.Type.KEYWORD_EXCEPTION,
        "keyword.debug" to HaskellSyntaxToken.Type.KEYWORD_DEBUG,
        "operator" to HaskellSyntaxToken.Type.OPERATOR,
        "punctuation.bracket" to HaskellSyntaxToken.Type.PUNCTUATION_BRACKET,
        "punctuation.delimiter" to HaskellSyntaxToken.Type.PUNCTUATION_DELIMITER,
        "string" to HaskellSyntaxToken.Type.STRING,
        "number" to HaskellSyntaxToken.Type.NUMBER,
        "number.float" to HaskellSyntaxToken.Type.NUMBER_FLOAT,
        "char" to HaskellSyntaxToken.Type.CHARACTER,
        "boolean" to HaskellSyntaxToken.Type.BOOLEAN,
        "comment" to HaskellSyntaxToken.Type.COMMENT,
        "comment.documentation" to HaskellSyntaxToken.Type.COMMENT_DOCUMENTATION
    )

    fun parseTokenType(captureName: String) = when (val capture = captureMap[captureName]) {
        null -> {
            var current = captureName

            while (current.contains(".")) {
                current = current.substringBeforeLast(".")

                if (captureMap.contains(current)) {
                    break
                }
            }

            captureMap[current] ?: HaskellSyntaxToken.Type.DEFAULT
        }

        else -> capture
    }
}