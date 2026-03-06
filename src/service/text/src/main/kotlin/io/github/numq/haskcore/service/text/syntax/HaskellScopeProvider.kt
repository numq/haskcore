package io.github.numq.haskcore.service.text.syntax

import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.service.text.toTSPoint
import io.github.numq.haskcore.service.text.toTextPosition
import org.treesitter.TSQueryCursor

internal class HaskellScopeProvider(
    private val queryProvider: QueryProvider, private val syntaxEngine: SyntaxEngine
) : ScopeProvider {
    override suspend fun getScopes(range: TextRange) = syntaxEngine.readTree { tree ->
        val localsQuery = queryProvider.localsQuery

        val syntaxScopes = mutableListOf<SyntaxScope>()

        val cursor = TSQueryCursor().apply {
            setPointRange(range.start.toTSPoint(), range.end.toTSPoint())
        }

        cursor.exec(localsQuery, tree.rootNode)

        cursor.matches.forEach { match ->
            match.captures.forEach { capture ->
                if (localsQuery.getCaptureNameForId(capture.index) == "local.scope") {
                    val node = capture.node

                    val start = node.startPoint.toTextPosition()

                    val end = node.endPoint.toTextPosition()

                    val range = TextRange(start = start, end = end)

                    if (range.isNotEmpty) {
                        syntaxScopes.add(SyntaxScope(range))
                    }
                }
            }
        }

        syntaxScopes.distinct().sortedBy { scope ->
            scope.range.start.line
        }
    }
}