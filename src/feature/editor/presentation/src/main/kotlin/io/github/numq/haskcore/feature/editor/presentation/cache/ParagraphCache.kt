package io.github.numq.haskcore.feature.editor.presentation.cache

import io.github.numq.haskcore.feature.editor.core.syntax.Token
import io.github.numq.haskcore.platform.font.EditorFont
import io.github.numq.haskcore.platform.theme.editor.EditorTheme
import io.github.numq.haskcore.platform.theme.editor.palette.highlighting.HighlightingColorPalette
import org.jetbrains.skia.paragraph.Paragraph
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.jetbrains.skia.paragraph.TextStyle

internal class ParagraphCache(override val capacity: Int) : LruCache<ParagraphCache.Key, Paragraph>() {
    data class Key(
        val text: String, val tokens: List<Token>, val width: Float, val font: EditorFont, val theme: EditorTheme
    )

    private fun HighlightingColorPalette.getColorForHighlightingToken(type: Token.Type) = when (type) {
        Token.Type.KEYWORD, Token.Type.KEYWORD_CONDITIONAL, Token.Type.KEYWORD_REPEAT, Token.Type.KEYWORD_EXCEPTION, Token.Type.KEYWORD_DEBUG -> keywordColor

        Token.Type.KEYWORD_IMPORT, Token.Type.KEYWORD_DIRECTIVE -> keywordImportColor

        Token.Type.TYPE, Token.Type.CONSTRUCTOR -> typeColor

        Token.Type.BOOLEAN -> booleanColor

        Token.Type.FUNCTION -> functionColor

        Token.Type.FUNCTION_CALL -> functionCallColor

        Token.Type.VARIABLE -> variableColor

        Token.Type.VARIABLE_PARAMETER -> variableParameterColor

        Token.Type.VARIABLE_MEMBER -> variableMemberColor

        Token.Type.OPERATOR -> operatorColor

        Token.Type.NUMBER, Token.Type.NUMBER_FLOAT -> numberColor

        Token.Type.STRING, Token.Type.CHARACTER -> stringColor

        Token.Type.STRING_SPECIAL_SYMBOL -> stringSpecialSymbolColor

        Token.Type.COMMENT -> commentColor

        Token.Type.COMMENT_DOCUMENTATION -> commentDocumentationColor

        Token.Type.PUNCTUATION_BRACKET -> punctuationBracketColor

        Token.Type.PUNCTUATION_DELIMITER -> punctuationDelimiterColor

        Token.Type.MODULE -> moduleColor

        Token.Type.SPELL -> spellColor

        Token.Type.WILDCARD -> wildcardColor

        Token.Type.UNKNOWN -> unknownColor

        Token.Type.LOCAL_DEFINITION -> localDefinitionColor

        Token.Type.LOCAL_REFERENCE -> localReferenceColor
    }

    override val factory: Key.() -> Paragraph = {
        val fontFamily = font.familyName

        val baseTextStyle = TextStyle().apply {
            setFontFamily(fontFamily)

            fontSize = font.size
        }

        val style = ParagraphStyle().apply {
            maxLinesCount = 1

            textStyle = baseTextStyle
        }

        val builder = font.buildParagraph(style) {
            fun createTokenStyle(color: Int) = TextStyle().apply {
                fontFamilies = baseTextStyle.fontFamilies

                fontSize = baseTextStyle.fontSize

                this.color = color
            }

            when {
                tokens.isEmpty() -> {
                    pushStyle(createTokenStyle(theme.codeAreaColorPalette.textColor))

                    addText(text)
                }

                else -> tokens.forEach { token ->
                    val color = theme.highlightingColorPalette.getColorForHighlightingToken(token.type)

                    pushStyle(createTokenStyle(color))

                    val text = when (token) {
                        is Token.Atom -> token.text

                        is Token.Region -> {
                            val start = token.range.start.column

                            val end = token.range.end.column

                            text.substring(start.coerceIn(0, text.length), end.coerceIn(0, text.length))
                        }
                    }

                    addText(text)

                    popStyle()
                }
            }
        }

        builder.build().apply { layout(Float.MAX_VALUE) }
    }
}