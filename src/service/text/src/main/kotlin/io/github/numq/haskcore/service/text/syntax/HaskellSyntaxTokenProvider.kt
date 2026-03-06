package io.github.numq.haskcore.service.text.syntax

import arrow.core.right
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.service.text.toTSPoint
import io.github.numq.haskcore.service.text.toTextPosition
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TSQueryCursor

internal class HaskellSyntaxTokenProvider(
    private val queryProvider: QueryProvider, private val syntaxEngine: SyntaxEngine
) : SyntaxTokenProvider {
    private fun SyntaxTokenType.isMultilineAllowed() = when (this) {
        SyntaxTokenType.COMMENT, SyntaxTokenType.COMMENT_DOCUMENTATION, SyntaxTokenType.STRING -> true

        else -> false
    }

    private fun collectTokensFromQuery(query: TSQuery, rootNode: TSNode, cursor: TSQueryCursor): List<SyntaxToken> {
        val result = mutableListOf<SyntaxToken>()

        cursor.exec(query, rootNode)

        cursor.matches.forEach { match ->
            match.captures.forEach { capture ->
                val captureName = query.getCaptureNameForId(capture.index)

                if (captureName == "local.scope" || captureName.contains("definition.import")) return@forEach

                val type = SyntaxTokenMapper.parseSyntax(captureName)

                if (type == SyntaxTokenType.DEFAULT) return@forEach

                val node = capture.node

                val tokenRange = TextRange(
                    start = node.startPoint.toTextPosition(), end = node.endPoint.toTextPosition()
                )

                result.add(SyntaxToken(range = tokenRange, type = type))
            }
        }
        return result
    }

    override suspend fun getSyntaxTokens(range: TextRange) = when {
        range.isEmpty -> emptyList<SyntaxToken>().right()

        else -> syntaxEngine.readTree { tree ->
            val rootNode = tree.rootNode

            val cursor = TSQueryCursor().apply {
                setPointRange(range.start.toTSPoint(), range.end.toTSPoint())
            }

            val (syntaxTokens, localTokens) = supervisorScope {
                val syntaxTokens = async {
                    collectTokensFromQuery(query = queryProvider.highlightsQuery, rootNode = rootNode, cursor = cursor)
                }

                val localTokens = async {
                    collectTokensFromQuery(query = queryProvider.localsQuery, rootNode = rootNode, cursor = cursor)
                }

                syntaxTokens.await() to localTokens.await()
            }

            (syntaxTokens + localTokens).filter { token ->
                token.range.start.line == token.range.end.line || token.type.isMultilineAllowed()
            }.sortedWith(SyntaxTokenComparator).distinctBy(SyntaxToken::range)
        }
    }
}