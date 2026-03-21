package io.github.numq.haskcore.service.syntax.symbol

import arrow.core.Either
import arrow.core.getOrElse
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.core.text.TextSnapshot
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TSQueryCursor
import org.treesitter.TSTree

internal class HaskellSymbolIndexer(private val symbolTable: SymbolTable) : SymbolIndexer {
    private companion object {
        const val FIELD_NAME = "name"

        const val TYPE_VARIABLE = "variable"

        const val LOCAL_DEFINITION = "local.definition"

        const val LOCAL_REFERENCE = "local.reference"
    }

    private fun TSNode.toTextRange(): TextRange {
        val start = TextPosition(line = startPoint.row, column = startPoint.column)

        val end = TextPosition(line = endPoint.row, column = endPoint.column)

        return TextRange(start = start, end = end)
    }

    private fun TextSnapshot.isRangeValid(range: TextRange) = when {
        range.start.line < 0 || range.start.line >= lines -> false

        else -> {
            val lineLength = getLineLength(line = range.start.line)

            range.start.column <= lineLength && range.end.column <= lineLength
        }
    }

    private fun getIdentifierRange(node: TSNode): TextRange {
        val nameNode = node.getChildByFieldName(FIELD_NAME)

        return when {
            !nameNode.isNull -> nameNode.toTextRange()

            else -> {
                val variableChild = (0 until node.childCount).map(node::getChild).firstOrNull { node ->
                    node.type == TYPE_VARIABLE
                }

                variableChild?.toTextRange() ?: node.toTextRange()
            }
        }
    }

    override suspend fun reindex(
        tree: TSTree, query: TSQuery, snapshot: TextSnapshot, dirtyRange: TextRange?
    ) = Either.catch {
        val cursor = TSQueryCursor()

        when (dirtyRange) {
            null -> symbolTable.clear().getOrElse { throwable -> throw throwable }

            else -> symbolTable.removeInRange(range = dirtyRange).getOrElse { throwable -> throw throwable }
        }

        cursor.exec(query, tree.rootNode)

        val processedRanges = mutableSetOf<TextRange>()

        cursor.matches.forEach { match ->
            match.captures.forEach { capture ->
                val node = capture.node

                val captureName = query.getCaptureNameForId(capture.index)

                if (captureName == LOCAL_DEFINITION || captureName == LOCAL_REFERENCE) {
                    val range = getIdentifierRange(node = node)

                    if (snapshot.isRangeValid(range = range) && processedRanges.add(range)) {
                        val name = snapshot.getTextInRange(range = range)

                        when (captureName) {
                            LOCAL_DEFINITION -> Symbol.Definition(name = name, range = range)

                            LOCAL_REFERENCE -> Symbol.Reference(name = name, range = range)

                            else -> null
                        }?.let { symbol ->
                            symbolTable.add(symbol = symbol).getOrElse { throwable -> throw throwable }
                        }
                    }
                }
            }
        }
    }
}