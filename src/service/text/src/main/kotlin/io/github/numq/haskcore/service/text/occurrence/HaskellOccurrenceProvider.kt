package io.github.numq.haskcore.service.text.occurrence

import arrow.core.flatten
import arrow.core.right
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.core.text.TextSnapshot
import io.github.numq.haskcore.service.text.syntax.QueryProvider
import io.github.numq.haskcore.service.text.syntax.SyntaxEngine
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.treesitter.TSNode
import org.treesitter.TSPoint
import org.treesitter.TSQueryCursor

internal class HaskellOccurrenceProvider(
    private val syntaxEngine: SyntaxEngine, private val queryProvider: QueryProvider
) : OccurrenceProvider {
    private fun TSNode.toTextRange(snapshot: TextSnapshot): TextRange {
        val start = snapshot.getTextPosition(bytePosition = startByte) ?: TextPosition.ZERO

        val end = snapshot.getTextPosition(bytePosition = endByte) ?: start

        return TextRange(start = start, end = end)
    }

    override suspend fun getLocalOccurrences(
        snapshot: TextSnapshot, position: TextPosition
    ) = syntaxEngine.readTree { tree ->
        val tsPoint = TSPoint(position.line, position.column)

        when (val targetNode = tree.rootNode.getDescendantForPointRange(tsPoint, tsPoint)) {
            null -> emptyList<Occurrence>().right()

            else -> {
                val localsQuery = queryProvider.localsQuery

                val targetRange = targetNode.toTextRange(snapshot = snapshot)

                val targetText = snapshot.getTextInRange(range = targetRange)

                val occurrences = mutableListOf<Occurrence>()

                val cursor = TSQueryCursor()

                cursor.exec(localsQuery, tree.rootNode)

                cursor.matches.forEach { match ->
                    match.captures.forEach { capture ->
                        val node = capture.node

                        val name = localsQuery.getCaptureNameForId(capture.index)

                        if ((node.endByte - node.startByte) == (targetNode.endByte - targetNode.startByte)) {
                            val nodeRange = node.toTextRange(snapshot = snapshot)

                            val nodeText = snapshot.getTextInRange(range = nodeRange)

                            if (nodeText == targetText) {
                                when {
                                    name.contains("definition") -> occurrences.add(
                                        Occurrence.Definition(
                                            nodeRange
                                        )
                                    )

                                    name.contains("reference") -> occurrences.add(Occurrence.Reference(nodeRange))
                                }
                            }
                        }
                    }
                }

                occurrences.distinctBy(Occurrence::range).right()
            }
        }
    }.flatten()

    override suspend fun getGlobalOccurrences(
        snapshot: TextSnapshot, position: TextPosition, references: List<TextRange>
    ) = getLocalOccurrences(snapshot = snapshot, position = position).map { occurrences ->
        flow {
            emitAll(occurrences.asFlow())

            references.forEach { range ->
                val startLine = range.start.line

                val startColumn = range.start.column

                val start = TextPosition(line = startLine, column = startColumn)

                val endLine = range.end.line

                val endColumn = range.end.column

                val end = TextPosition(line = endLine, column = endColumn)

                val range = TextRange(start = start, end = end)

                val occurrence = Occurrence.Reference(range = range)

                emit(occurrence)
            }
        }.distinctUntilChanged { old, new -> old.range == new.range }
    }
}