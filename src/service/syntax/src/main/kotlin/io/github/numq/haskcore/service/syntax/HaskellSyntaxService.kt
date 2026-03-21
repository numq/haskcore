package io.github.numq.haskcore.service.syntax

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import io.github.numq.haskcore.core.text.TextEdit
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.core.text.TextSnapshot
import io.github.numq.haskcore.service.syntax.folding.SyntaxFoldingProvider
import io.github.numq.haskcore.service.syntax.occurrence.SyntaxOccurrenceProvider
import io.github.numq.haskcore.service.syntax.query.SyntaxQueryProvider
import io.github.numq.haskcore.service.syntax.symbol.SymbolIndexer
import io.github.numq.haskcore.service.syntax.token.SyntaxTokenProvider
import io.github.numq.haskcore.service.syntax.tree.SyntaxTree
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.treesitter.TSInputEdit
import org.treesitter.TSParser

@OptIn(ExperimentalCoroutinesApi::class)
internal class HaskellSyntaxService(
    private val scope: CoroutineScope,
    private val foldingProvider: SyntaxFoldingProvider,
    private val occurrenceProvider: SyntaxOccurrenceProvider,
    private val symbolIndexer: SymbolIndexer,
    private val syntaxTokenProvider: SyntaxTokenProvider,
    private val queryProvider: SyntaxQueryProvider
) : SyntaxService {
    private val dispatcher = Dispatchers.Default.limitedParallelism(1)

    private val parser = TSParser().apply { language = queryProvider.language }

    private val _syntaxTree = MutableStateFlow<SyntaxTree?>(null)

    override val syntax = _syntaxTree.filter { syntaxTree ->
        syntaxTree?.revision == syntaxTree?.syntax?.revision
    }.map { syntaxTree ->
        syntaxTree?.syntax
    }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = null)

    override suspend fun fullParse(snapshot: TextSnapshot) = Either.catch {
        withContext(dispatcher) {
            val revision = snapshot.revision

            _syntaxTree.getAndUpdate { syntaxTree ->
                val newTree = parser.parseString(null, snapshot.text)

                symbolIndexer.reindex(
                    tree = newTree, query = queryProvider.localsQuery, snapshot = snapshot, dirtyRange = null
                ).getOrElse { throwable ->
                    throw throwable
                }

                SyntaxTree(
                    revision = revision, tree = newTree, syntax = Syntax(revision = revision, text = snapshot.text)
                )
            }?.takeUnless(SyntaxTree::isClosed)?.tree?.close() ?: Unit
        }
    }

    override suspend fun applyChange(
        snapshot: TextSnapshot, data: TextEdit.Data, range: TextRange, position: TextPosition
    ) = Either.catch {
        withContext(dispatcher) {
            val oldTree = _syntaxTree.value?.takeUnless(SyntaxTree::isClosed)?.tree ?: return@withContext

            val revision = snapshot.revision

            val inputEdit = with(data) {
                TSInputEdit(
                    startByte,
                    oldEndByte,
                    newEndByte,
                    startPosition.toTSPoint(),
                    oldEndPosition.toTSPoint(),
                    newEndPosition.toTSPoint()
                )
            }

            oldTree.edit(inputEdit)

            val newTree = checkNotNull(parser.parseString(oldTree, snapshot.text)) { "Failed to parse Haskell tree" }

            try {
                val dirtyRange = TextRange(start = data.startPosition, end = data.newEndPosition)

                symbolIndexer.reindex(
                    tree = newTree, query = queryProvider.localsQuery, snapshot = snapshot, dirtyRange = dirtyRange
                ).getOrElse { throwable ->
                    throw throwable
                }

                _syntaxTree.getAndUpdate { currentSyntaxTree ->
                    currentSyntaxTree?.copy(
                        revision = revision, tree = newTree, syntax = currentSyntaxTree.syntax.copy(revision = revision)
                    )
                }?.takeUnless(SyntaxTree::isClosed)?.tree?.close()
            } catch (throwable: Throwable) {
                newTree.close()

                throw throwable
            }
        }
    }

    override suspend fun parseFoldingRegions(range: TextRange) = either {
        val currentSyntaxTree = _syntaxTree.value?.takeUnless(SyntaxTree::isClosed) ?: return@either

        val tree = currentSyntaxTree.tree

        withContext(dispatcher) {
            val foldingRegions = foldingProvider.getSyntaxFoldingRegions(
                tree = tree, localsQuery = queryProvider.localsQuery, range = range
            ).bind()

            _syntaxTree.update { latestSyntaxTree ->
                when (latestSyntaxTree?.revision) {
                    currentSyntaxTree.revision -> latestSyntaxTree.copy(
                        syntax = latestSyntaxTree.syntax.copy(
                            foldingRegions = foldingRegions
                        )
                    )

                    else -> latestSyntaxTree
                }
            }
        }
    }

    override suspend fun parseOccurrences(position: TextPosition) = either {
        val currentSyntaxTree = _syntaxTree.value?.takeUnless(SyntaxTree::isClosed) ?: return@either

        withContext(dispatcher) {
            val occurrences = occurrenceProvider.getSyntaxOccurrences(position = position).bind()

            _syntaxTree.update { latestSyntaxTree ->
                when (latestSyntaxTree?.revision) {
                    currentSyntaxTree.revision -> latestSyntaxTree.copy(
                        syntax = latestSyntaxTree.syntax.copy(
                            occurrences = occurrences
                        )
                    )

                    else -> latestSyntaxTree
                }
            }
        }
    }

    override suspend fun parseTokensPerLine(snapshot: TextSnapshot, range: TextRange) = either {
        val currentSyntaxTree = _syntaxTree.value?.takeUnless(SyntaxTree::isClosed) ?: return@either

        val tree = currentSyntaxTree.tree

        withContext(dispatcher) {
            val tokensPerLine = syntaxTokenProvider.getSyntaxTokensPerLine(
                tree = tree,
                highlightsQuery = queryProvider.highlightsQuery,
                localsQuery = queryProvider.localsQuery,
                lineLengths = (range.start.line..range.end.line).associateWith(snapshot::getLineLength),
                range = range
            ).bind()

            _syntaxTree.update { latestSyntaxTree ->
                when (latestSyntaxTree?.revision) {
                    currentSyntaxTree.revision -> latestSyntaxTree.copy(
                        syntax = latestSyntaxTree.syntax.copy(
                            tokensPerLine = tokensPerLine
                        )
                    )

                    else -> latestSyntaxTree
                }
            }
        }
    }

    override fun close() {
        scope.cancel()

        _syntaxTree.getAndUpdate { null }?.close()

        parser.close()
    }
}