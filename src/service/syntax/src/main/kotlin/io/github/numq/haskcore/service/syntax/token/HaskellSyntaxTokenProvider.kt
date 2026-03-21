package io.github.numq.haskcore.service.syntax.token

import arrow.core.Either
import arrow.core.right
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.service.syntax.toTSPoint
import io.github.numq.haskcore.service.syntax.toTextPosition
import io.github.numq.haskcore.service.syntax.token.HaskellSyntaxToken.Type
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TSQueryCursor
import org.treesitter.TSTree

internal class HaskellSyntaxTokenProvider : SyntaxTokenProvider {
    private fun fillGapsInLine(
        lineIndex: Int, lineLength: Int, lineRegions: List<SyntaxToken.Region>
    ) = when (lineLength) {
        0 -> emptyList()

        else -> {
            val result = ArrayList<SyntaxToken>(lineRegions.size * 2 + 1)

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
                        SyntaxToken.Region(
                            range = TextRange(
                                start = TextPosition(line = lineIndex, column = currentColumn),
                                end = TextPosition(line = lineIndex, column = safeStart)
                            ), type = SyntaxToken.Type.UNKNOWN
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
                    SyntaxToken.Region(
                        range = TextRange(
                            start = TextPosition(line = lineIndex, column = currentColumn),
                            end = TextPosition(line = lineIndex, column = lineLength)
                        ), type = SyntaxToken.Type.UNKNOWN
                    )
                )
            }

            result
        }
    }

    private fun collectHaskellTokensFromQuery(
        query: TSQuery, rootNode: TSNode, cursor: TSQueryCursor
    ): List<HaskellSyntaxToken> {
        val result = mutableListOf<HaskellSyntaxToken>()

        cursor.exec(query, rootNode)

        cursor.matches.forEach { match ->
            match.captures.forEach { capture ->
                val captureName = query.getCaptureNameForId(capture.index)

                when {
                    captureName == "local.scope" || captureName.contains("definition.import") -> return@forEach

                    else -> when (val type = SyntaxTokenMapper.parseTokenType(captureName = captureName)) {
                        Type.DEFAULT -> return@forEach

                        else -> {
                            val node = capture.node

                            val tokenRange = TextRange(
                                start = node.startPoint.toTextPosition(), end = node.endPoint.toTextPosition()
                            )

                            result.add(HaskellSyntaxToken(range = tokenRange, type = type))
                        }
                    }
                }
            }
        }

        return result
    }


    override suspend fun getSyntaxTokensPerLine(
        tree: TSTree, highlightsQuery: TSQuery, localsQuery: TSQuery, lineLengths: Map<Int, Int>, range: TextRange
    ) = when {
        range.isEmpty -> emptyMap<Int, List<SyntaxToken>>().right()

        else -> Either.catch {
            val rootNode = tree.rootNode

            val (highlightTokens, localTokens) = supervisorScope {
                val highlightTokensAsync = async {
                    TSQueryCursor().apply {
                        setPointRange(range.start.toTSPoint(), range.end.toTSPoint())
                    }.use { cursor ->
                        collectHaskellTokensFromQuery(query = highlightsQuery, rootNode = rootNode, cursor = cursor)
                    }
                }

                val localTokensAsync = async {
                    TSQueryCursor().apply {
                        setPointRange(range.start.toTSPoint(), range.end.toTSPoint())
                    }.use { cursor ->
                        collectHaskellTokensFromQuery(query = localsQuery, rootNode = rootNode, cursor = cursor)
                    }
                }

                highlightTokensAsync.await() to localTokensAsync.await()
            }

            val tokens = (highlightTokens + localTokens).mapNotNull { haskellToken ->
                when {
                    haskellToken.range.start.line == haskellToken.range.end.line || haskellToken.type.isMultilineAllowed() -> SyntaxToken.Region(
                        range = haskellToken.range, type = when (haskellToken.type) {
                            Type.VARIABLE, Type.VARIABLE_ID -> SyntaxToken.Type.VARIABLE

                            Type.VARIABLE_PARAMETER -> SyntaxToken.Type.VARIABLE_PARAMETER

                            Type.VARIABLE_MEMBER -> SyntaxToken.Type.VARIABLE_MEMBER

                            Type.FUNCTION -> SyntaxToken.Type.FUNCTION

                            Type.FUNCTION_CALL, Type.FUNCTION_INFIX -> SyntaxToken.Type.FUNCTION_CALL

                            Type.TYPE, Type.TYPE_VARIABLE -> SyntaxToken.Type.TYPE

                            Type.CONSTRUCTOR -> SyntaxToken.Type.CONSTRUCTOR

                            Type.MODULE -> SyntaxToken.Type.MODULE

                            Type.KEYWORD -> SyntaxToken.Type.KEYWORD

                            Type.KEYWORD_IMPORT -> SyntaxToken.Type.KEYWORD_IMPORT

                            Type.KEYWORD_CONDITIONAL -> SyntaxToken.Type.KEYWORD_CONDITIONAL

                            Type.KEYWORD_REPEAT -> SyntaxToken.Type.KEYWORD_REPEAT

                            Type.KEYWORD_DIRECTIVE -> SyntaxToken.Type.KEYWORD_DIRECTIVE

                            Type.KEYWORD_EXCEPTION -> SyntaxToken.Type.KEYWORD_EXCEPTION

                            Type.KEYWORD_DEBUG -> SyntaxToken.Type.KEYWORD_DEBUG

                            Type.STRING, Type.STRING_ESCAPE, Type.QUASI_QUOTE -> SyntaxToken.Type.STRING

                            Type.NUMBER -> SyntaxToken.Type.NUMBER

                            Type.NUMBER_FLOAT -> SyntaxToken.Type.NUMBER_FLOAT

                            Type.CHARACTER -> SyntaxToken.Type.CHARACTER

                            Type.BOOLEAN -> SyntaxToken.Type.BOOLEAN

                            Type.OPERATOR -> SyntaxToken.Type.OPERATOR

                            Type.PUNCTUATION_BRACKET -> SyntaxToken.Type.PUNCTUATION_BRACKET

                            Type.PUNCTUATION_DELIMITER -> SyntaxToken.Type.PUNCTUATION_DELIMITER

                            Type.COMMENT -> SyntaxToken.Type.COMMENT

                            Type.COMMENT_DOCUMENTATION -> SyntaxToken.Type.COMMENT_DOCUMENTATION

                            Type.DEFAULT -> SyntaxToken.Type.UNKNOWN
                        }
                    )

                    else -> null
                }
            }.sortedWith(SyntaxTokenComparator).distinctBy(SyntaxToken::range)

            val tokensByLine = buildMap {
                tokens.forEach { token ->
                    val startLine = token.range.start.line

                    val endLine = token.range.end.line

                    for (line in startLine..endLine) {
                        getOrPut(line) { ArrayList() }.add(token)
                    }
                }
            }

            buildMap {
                for (lineIndex in range.start.line..range.end.line) {
                    lineLengths[lineIndex]?.let { lineLength ->
                        val tokensForLine = tokensByLine[lineIndex].orEmpty()

                        put(
                            lineIndex,
                            fillGapsInLine(lineIndex = lineIndex, lineLength = lineLength, lineRegions = tokensForLine)
                        )
                    }
                }
            }
        }
    }
}