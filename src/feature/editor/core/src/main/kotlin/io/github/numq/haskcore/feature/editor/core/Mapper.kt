package io.github.numq.haskcore.feature.editor.core

import io.github.numq.haskcore.feature.editor.core.analysis.CodeDocumentation
import io.github.numq.haskcore.feature.editor.core.analysis.CodeIssue
import io.github.numq.haskcore.feature.editor.core.analysis.CodeSuggestion
import io.github.numq.haskcore.feature.editor.core.syntax.Occurrence
import io.github.numq.haskcore.feature.editor.core.token.Token
import io.github.numq.haskcore.service.lsp.completion.LspCompletion
import io.github.numq.haskcore.service.lsp.diagnostic.LspDiagnostic
import io.github.numq.haskcore.service.lsp.hover.LspHover
import io.github.numq.haskcore.service.lsp.token.LspToken
import io.github.numq.haskcore.service.syntax.occurrence.SyntaxOccurrence
import io.github.numq.haskcore.service.syntax.token.SyntaxToken

internal fun EditorPositionData.toEditorPosition() = EditorPosition(
    horizontalOffset = horizontalOffset, verticalOffset = verticalOffset
)

internal fun EditorPosition.toEditorPositionData() = EditorPositionData(
    horizontalOffset = horizontalOffset, verticalOffset = verticalOffset
)

internal fun LspHover.toDocumentation() = CodeDocumentation(content = content, range = range)

internal fun LspCompletion.toCodeSuggestion(): CodeSuggestion {
    val kind = when (kind) {
        LspCompletion.Kind.METHOD -> CodeSuggestion.Kind.METHOD

        LspCompletion.Kind.FUNCTION -> CodeSuggestion.Kind.FUNCTION

        LspCompletion.Kind.CONSTRUCTOR -> CodeSuggestion.Kind.CONSTRUCTOR

        LspCompletion.Kind.FIELD -> CodeSuggestion.Kind.FIELD

        LspCompletion.Kind.VARIABLE -> CodeSuggestion.Kind.VARIABLE

        LspCompletion.Kind.CLASS -> CodeSuggestion.Kind.CLASS

        LspCompletion.Kind.INTERFACE -> CodeSuggestion.Kind.INTERFACE

        LspCompletion.Kind.MODULE -> CodeSuggestion.Kind.MODULE

        LspCompletion.Kind.PROPERTY -> CodeSuggestion.Kind.PROPERTY

        LspCompletion.Kind.UNIT -> CodeSuggestion.Kind.UNIT

        LspCompletion.Kind.VALUE -> CodeSuggestion.Kind.VALUE

        LspCompletion.Kind.ENUM -> CodeSuggestion.Kind.ENUM

        LspCompletion.Kind.KEYWORD -> CodeSuggestion.Kind.KEYWORD

        LspCompletion.Kind.SNIPPET -> CodeSuggestion.Kind.SNIPPET
    }

    return CodeSuggestion(
        label = label,
        kind = kind,
        text = insertText,
        detail = detail,
        documentation = documentation,
        range = textEditRange
    )
}

internal fun LspDiagnostic.toCodeIssue(): CodeIssue = when (this) {
    is LspDiagnostic.Unknown -> CodeIssue.Unknown(range = range, message = message, source = source, code = code)

    is LspDiagnostic.Error -> CodeIssue.Error(range = range, message = message, source = source, code = code)

    is LspDiagnostic.Warning -> CodeIssue.Warning(range = range, message = message, source = source, code = code)

    is LspDiagnostic.Information -> CodeIssue.Information(
        range = range, message = message, source = source, code = code
    )

    is LspDiagnostic.Hint -> CodeIssue.Hint(range = range, message = message, source = source, code = code)
}

internal fun LspToken.Type.toTokenType() = when (this) {
    LspToken.Type.NAMESPACE -> Token.Type.MODULE

    LspToken.Type.TYPE -> Token.Type.TYPE

    LspToken.Type.CLASS -> Token.Type.CONSTRUCTOR

    LspToken.Type.ENUM -> Token.Type.CONSTRUCTOR

    LspToken.Type.INTERFACE -> Token.Type.TYPE

    LspToken.Type.STRUCT -> Token.Type.TYPE

    LspToken.Type.TYPE_PARAMETER -> Token.Type.VARIABLE_PARAMETER

    LspToken.Type.PARAMETER -> Token.Type.VARIABLE_PARAMETER

    LspToken.Type.VARIABLE -> Token.Type.VARIABLE

    LspToken.Type.PROPERTY -> Token.Type.VARIABLE_MEMBER

    LspToken.Type.ENUM_MEMBER -> Token.Type.CONSTRUCTOR

    LspToken.Type.EVENT -> Token.Type.FUNCTION_CALL

    LspToken.Type.FUNCTION -> Token.Type.FUNCTION

    LspToken.Type.METHOD -> Token.Type.FUNCTION

    LspToken.Type.MACRO -> Token.Type.KEYWORD_DIRECTIVE

    LspToken.Type.KEYWORD -> Token.Type.KEYWORD

    LspToken.Type.MODIFIER -> Token.Type.KEYWORD

    LspToken.Type.COMMENT -> Token.Type.COMMENT

    LspToken.Type.STRING -> Token.Type.STRING

    LspToken.Type.NUMBER -> Token.Type.NUMBER

    LspToken.Type.REGEXP -> Token.Type.STRING_SPECIAL_SYMBOL

    LspToken.Type.OPERATOR -> Token.Type.OPERATOR
}

internal fun LspToken.toToken() = Token.Region(range = range, type = type.toTokenType())

internal fun SyntaxToken.Type.toTokenType() = when (this) {
    SyntaxToken.Type.KEYWORD -> Token.Type.KEYWORD
    SyntaxToken.Type.KEYWORD_CONDITIONAL -> Token.Type.KEYWORD_CONDITIONAL

    SyntaxToken.Type.KEYWORD_IMPORT -> Token.Type.KEYWORD_IMPORT

    SyntaxToken.Type.KEYWORD_REPEAT -> Token.Type.KEYWORD_REPEAT

    SyntaxToken.Type.KEYWORD_DIRECTIVE -> Token.Type.KEYWORD_DIRECTIVE

    SyntaxToken.Type.KEYWORD_EXCEPTION -> Token.Type.KEYWORD_EXCEPTION

    SyntaxToken.Type.KEYWORD_DEBUG -> Token.Type.KEYWORD_DEBUG

    SyntaxToken.Type.TYPE -> Token.Type.TYPE

    SyntaxToken.Type.CONSTRUCTOR -> Token.Type.CONSTRUCTOR

    SyntaxToken.Type.BOOLEAN -> Token.Type.BOOLEAN

    SyntaxToken.Type.FUNCTION -> Token.Type.FUNCTION

    SyntaxToken.Type.FUNCTION_CALL -> Token.Type.FUNCTION_CALL

    SyntaxToken.Type.VARIABLE -> Token.Type.VARIABLE

    SyntaxToken.Type.VARIABLE_PARAMETER -> Token.Type.VARIABLE_PARAMETER

    SyntaxToken.Type.VARIABLE_MEMBER -> Token.Type.VARIABLE_MEMBER

    SyntaxToken.Type.OPERATOR -> Token.Type.OPERATOR

    SyntaxToken.Type.NUMBER -> Token.Type.NUMBER

    SyntaxToken.Type.NUMBER_FLOAT -> Token.Type.NUMBER_FLOAT

    SyntaxToken.Type.STRING -> Token.Type.STRING

    SyntaxToken.Type.CHARACTER -> Token.Type.CHARACTER

    SyntaxToken.Type.STRING_SPECIAL_SYMBOL -> Token.Type.STRING_SPECIAL_SYMBOL

    SyntaxToken.Type.COMMENT -> Token.Type.COMMENT

    SyntaxToken.Type.COMMENT_DOCUMENTATION -> Token.Type.COMMENT_DOCUMENTATION

    SyntaxToken.Type.PUNCTUATION_BRACKET -> Token.Type.PUNCTUATION_BRACKET

    SyntaxToken.Type.PUNCTUATION_DELIMITER -> Token.Type.PUNCTUATION_DELIMITER

    SyntaxToken.Type.MODULE -> Token.Type.MODULE

    SyntaxToken.Type.SPELL -> Token.Type.SPELL

    SyntaxToken.Type.WILDCARD -> Token.Type.WILDCARD

    SyntaxToken.Type.LOCAL_DEFINITION -> Token.Type.LOCAL_DEFINITION

    SyntaxToken.Type.LOCAL_REFERENCE -> Token.Type.LOCAL_REFERENCE

    SyntaxToken.Type.UNKNOWN -> Token.Type.UNKNOWN
}

internal fun SyntaxToken.toToken(): Token = when (this) {
    is SyntaxToken.Atom -> Token.Atom(range = range, type = type.toTokenType(), text = text)

    is SyntaxToken.Region -> Token.Region(range = range, type = type.toTokenType())
}

internal fun SyntaxOccurrence.toOccurrence() = when (this) {
    is SyntaxOccurrence.Definition -> Occurrence.Definition(range = range)

    is SyntaxOccurrence.Reference -> Occurrence.Reference(range = range)
}