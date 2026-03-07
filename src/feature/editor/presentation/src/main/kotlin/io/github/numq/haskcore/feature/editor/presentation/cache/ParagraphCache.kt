package io.github.numq.haskcore.feature.editor.presentation.cache

import io.github.numq.haskcore.feature.editor.core.highlighting.HighlightingToken
import io.github.numq.haskcore.feature.editor.core.highlighting.HighlightingType
import io.github.numq.haskcore.platform.font.EditorFont
import io.github.numq.haskcore.platform.theme.editor.EditorTheme
import io.github.numq.haskcore.platform.theme.editor.palette.highlighting.HighlightingColorPalette
import org.jetbrains.skia.paragraph.Paragraph
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.jetbrains.skia.paragraph.TextStyle

internal class ParagraphCache(override val capacity: Int) : LruCache<ParagraphCache.Key, Paragraph>() {
    data class Key(
        val text: String,
        val tokens: List<HighlightingToken>,
        val width: Float,
        val font: EditorFont,
        val theme: EditorTheme
    )

    private fun HighlightingColorPalette.getColorForHighlightingToken(type: HighlightingType) = when (type) {
        HighlightingType.KEYWORD, HighlightingType.KEYWORD_CONDITIONAL, HighlightingType.KEYWORD_REPEAT, HighlightingType.KEYWORD_EXCEPTION, HighlightingType.KEYWORD_DEBUG -> keywordColor

        HighlightingType.KEYWORD_IMPORT, HighlightingType.KEYWORD_DIRECTIVE -> keywordImportColor

        HighlightingType.TYPE, HighlightingType.CONSTRUCTOR -> typeColor

        HighlightingType.BOOLEAN -> booleanColor

        HighlightingType.FUNCTION -> functionColor

        HighlightingType.FUNCTION_CALL -> functionCallColor

        HighlightingType.VARIABLE -> variableColor

        HighlightingType.VARIABLE_PARAMETER -> variableParameterColor

        HighlightingType.VARIABLE_MEMBER -> variableMemberColor

        HighlightingType.OPERATOR -> operatorColor

        HighlightingType.NUMBER, HighlightingType.NUMBER_FLOAT -> numberColor

        HighlightingType.STRING, HighlightingType.CHARACTER -> stringColor

        HighlightingType.STRING_SPECIAL_SYMBOL -> stringSpecialSymbolColor

        HighlightingType.COMMENT -> commentColor

        HighlightingType.COMMENT_DOCUMENTATION -> commentDocumentationColor

        HighlightingType.PUNCTUATION_BRACKET -> punctuationBracketColor

        HighlightingType.PUNCTUATION_DELIMITER -> punctuationDelimiterColor

        HighlightingType.MODULE -> moduleColor

        HighlightingType.SPELL -> spellColor

        HighlightingType.WILDCARD -> wildcardColor

        HighlightingType.UNKNOWN -> unknownColor

        HighlightingType.LOCAL_DEFINITION -> localDefinitionColor

        HighlightingType.LOCAL_REFERENCE -> localReferenceColor
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
                        is HighlightingToken.Atom -> token.text

                        is HighlightingToken.Region -> {
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