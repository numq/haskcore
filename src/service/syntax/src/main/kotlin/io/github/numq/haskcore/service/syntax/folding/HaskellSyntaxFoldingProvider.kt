package io.github.numq.haskcore.service.syntax.folding

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.service.syntax.toTSPoint
import io.github.numq.haskcore.service.syntax.toTextPosition
import org.treesitter.TSQuery
import org.treesitter.TSQueryCursor
import org.treesitter.TSTree

internal class HaskellSyntaxFoldingProvider : SyntaxFoldingProvider {
    override suspend fun getSyntaxFoldingRegions(tree: TSTree, localsQuery: TSQuery, range: TextRange) = Either.catch {
        val cursor = TSQueryCursor().apply {
            setPointRange(range.start.toTSPoint(), range.end.toTSPoint())
        }

        cursor.exec(localsQuery, tree.rootNode)

        buildList {
            cursor.matches.forEach { match ->
                match.captures.forEach { capture ->
                    if (localsQuery.getCaptureNameForId(capture.index) == "local.scope") {
                        val node = capture.node

                        val start = node.startPoint.toTextPosition()

                        val end = node.endPoint.toTextPosition()

                        val range = TextRange(start = start, end = end)

                        if (range.isNotEmpty) {
                            add(SyntaxFoldingRegion(range = range))
                        }
                    }
                }
            }
        }.distinct().sortedBy { foldingRegion ->
            foldingRegion.range.start.line
        }
    }
}