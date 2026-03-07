package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.feature.editor.core.highlighting.Highlighting
import io.github.numq.haskcore.feature.editor.core.highlighting.HighlightingScope
import io.github.numq.haskcore.feature.editor.core.highlighting.HighlightingToken
import io.github.numq.haskcore.feature.editor.core.highlighting.HighlightingType
import io.github.numq.haskcore.service.text.TextService
import io.github.numq.haskcore.service.text.syntax.SyntaxTokenType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class ObserveHighlighting(
    private val editorService: EditorService, private val textService: TextService
) : UseCase<Unit, Flow<Highlighting>> {
    private companion object {
        const val HIGHLIGHTING_LINE_PADDING = 10
    }

    private fun fillGapsInLine(
        lineIndex: Int, lineLength: Int, lineRegions: List<HighlightingToken.Region>
    ) = when (lineLength) {
        0 -> emptyList()

        else -> {
            val result = ArrayList<HighlightingToken>(lineRegions.size * 2 + 1)

            var currentColumn = 0

            val sortedRegions = lineRegions.sortedBy { highlightingRegion ->
                highlightingRegion.range.start.column
            }

            sortedRegions.forEach { region ->
                val startCol = when (region.range.start.line) {
                    lineIndex -> region.range.start.column

                    else -> 0
                }

                val endCol = when (region.range.end.line) {
                    lineIndex -> region.range.end.column

                    else -> lineLength
                }

                val safeStart = startCol.coerceIn(currentColumn, lineLength)

                val safeEnd = endCol.coerceIn(safeStart, lineLength)

                if (safeStart > currentColumn) {
                    result.add(
                        HighlightingToken.Region(
                            range = TextRange(
                                start = TextPosition(line = lineIndex, column = currentColumn),
                                end = TextPosition(line = lineIndex, column = safeStart)
                            ), type = HighlightingType.UNKNOWN
                        )
                    )
                }

                result.add(
                    region.copy(
                        range = TextRange(
                            start = TextPosition(line = lineIndex, column = safeStart),
                            end = TextPosition(line = lineIndex, column = safeEnd)
                        )
                    )
                )

                currentColumn = safeEnd
            }

            if (currentColumn < lineLength) {
                result.add(
                    HighlightingToken.Region(
                        range = TextRange(
                            start = TextPosition(line = lineIndex, column = currentColumn),
                            end = TextPosition(line = lineIndex, column = lineLength)
                        ), type = HighlightingType.UNKNOWN
                    )
                )
            }

            result
        }
    }

    private fun SyntaxTokenType.toHighlightingType() = when (this) {
        SyntaxTokenType.VARIABLE, SyntaxTokenType.VARIABLE_ID -> HighlightingType.VARIABLE

        SyntaxTokenType.VARIABLE_PARAMETER -> HighlightingType.VARIABLE_PARAMETER

        SyntaxTokenType.VARIABLE_FIELD -> HighlightingType.VARIABLE_MEMBER

        SyntaxTokenType.FUNCTION -> HighlightingType.FUNCTION

        SyntaxTokenType.FUNCTION_CALL, SyntaxTokenType.FUNCTION_INFIX -> HighlightingType.FUNCTION_CALL

        SyntaxTokenType.TYPE, SyntaxTokenType.TYPE_VARIABLE -> HighlightingType.TYPE

        SyntaxTokenType.CONSTRUCTOR -> HighlightingType.CONSTRUCTOR

        SyntaxTokenType.MODULE -> HighlightingType.MODULE

        SyntaxTokenType.KEYWORD -> HighlightingType.KEYWORD

        SyntaxTokenType.KEYWORD_IMPORT -> HighlightingType.KEYWORD_IMPORT

        SyntaxTokenType.KEYWORD_CONDITIONAL -> HighlightingType.KEYWORD_CONDITIONAL

        SyntaxTokenType.KEYWORD_REPEAT -> HighlightingType.KEYWORD_REPEAT

        SyntaxTokenType.KEYWORD_DIRECTIVE -> HighlightingType.KEYWORD_DIRECTIVE

        SyntaxTokenType.KEYWORD_EXCEPTION -> HighlightingType.KEYWORD_EXCEPTION

        SyntaxTokenType.KEYWORD_DEBUG -> HighlightingType.KEYWORD_DEBUG

        SyntaxTokenType.STRING, SyntaxTokenType.STRING_ESCAPE, SyntaxTokenType.QUASI_QUOTE -> HighlightingType.STRING

        SyntaxTokenType.NUMBER -> HighlightingType.NUMBER

        SyntaxTokenType.NUMBER_FLOAT -> HighlightingType.NUMBER_FLOAT

        SyntaxTokenType.CHARACTER -> HighlightingType.CHARACTER

        SyntaxTokenType.BOOLEAN -> HighlightingType.BOOLEAN

        SyntaxTokenType.OPERATOR -> HighlightingType.OPERATOR

        SyntaxTokenType.PUNCTUATION_BRACKET -> HighlightingType.PUNCTUATION_BRACKET

        SyntaxTokenType.PUNCTUATION_DELIMITER -> HighlightingType.PUNCTUATION_DELIMITER

        SyntaxTokenType.COMMENT -> HighlightingType.COMMENT

        SyntaxTokenType.COMMENT_DOCUMENTATION -> HighlightingType.COMMENT_DOCUMENTATION

        SyntaxTokenType.DEFAULT -> HighlightingType.UNKNOWN
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun Raise<Throwable>.execute(input: Unit) = combine(
        flow = editorService.highlightingRange.filterNot { range -> range.isEmpty() },
        flow2 = textService.snapshot.filterNotNull(),
        flow3 = textService.edits.onStart { emit(null) },
        transform = { range, snapshot, _ ->
            val startLine = (range.first - HIGHLIGHTING_LINE_PADDING).coerceIn(0, snapshot.lines - 1)

            val endLine = (range.last + HIGHLIGHTING_LINE_PADDING).coerceIn(0, snapshot.lines - 1)

            val startPosition = TextPosition(line = startLine, column = 0)

            val endPosition = TextPosition(line = endLine, column = snapshot.getLineText(line = endLine).length)

            val range = TextRange(start = startPosition, end = endPosition)

            snapshot to range
        }).conflate().map { (contentProvider, range) ->
        when {
            range.isEmpty -> Highlighting()

            else -> {
                val scopes = textService.getScopes(range = range).bind().map { syntaxScope ->
                    HighlightingScope(range = syntaxScope.range)
                }

                val tokens = textService.getSyntaxTokens(range = range).bind().map { syntaxToken ->
                    HighlightingToken.Region(
                        range = syntaxToken.range, type = syntaxToken.type.toHighlightingType()
                    )
                }

                val tokensByLine = buildMap {
                    tokens.forEach { token ->
                        val startLine = token.range.start.line

                        val endLine = token.range.end.line

                        for (line in startLine..endLine) {
                            getOrPut(line) { ArrayList() }.add(token)
                        }
                    }
                }

                val tokensPerLine = buildMap {
                    for (lineIndex in range.start.line..range.end.line) {
                        val lineLength = contentProvider.getLineLength(line = lineIndex)

                        val tokensForLine = tokensByLine[lineIndex] ?: emptyList()

                        put(
                            lineIndex,
                            fillGapsInLine(lineIndex = lineIndex, lineLength = lineLength, lineRegions = tokensForLine)
                        )
                    }
                }

                Highlighting(scopes = scopes, tokensPerLine = tokensPerLine)
            }
        }
    }
}