package io.github.numq.haskcore.service.syntax

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import io.github.numq.haskcore.common.core.text.TextEdit
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.core.text.TextRange
import io.github.numq.haskcore.common.core.text.TextSnapshot
import io.github.numq.haskcore.service.syntax.folding.SyntaxFoldingProvider
import io.github.numq.haskcore.service.syntax.occurrence.SyntaxOccurrenceProvider
import io.github.numq.haskcore.service.syntax.query.SyntaxQueryProvider
import io.github.numq.haskcore.service.syntax.symbol.SymbolIndexer
import io.github.numq.haskcore.service.syntax.token.SyntaxTokenProvider
import io.github.numq.haskcore.service.syntax.tree.SyntaxTree
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.treesitter.TSInputEdit
import org.treesitter.TSInputEncoding
import org.treesitter.TSParser

internal class HaskellSyntaxService(
    private val scope: CoroutineScope,
    private val foldingProvider: SyntaxFoldingProvider,
    private val occurrenceProvider: SyntaxOccurrenceProvider,
    private val symbolIndexer: SymbolIndexer,
    private val syntaxTokenProvider: SyntaxTokenProvider,
    private val queryProvider: SyntaxQueryProvider,
) : SyntaxService {
    private val lock = Mutex()

    private val dispatcher = Dispatchers.Default.limitedParallelism(1)

    private val encoding = TSInputEncoding.TSInputEncodingUTF8

    private val parser = TSParser().apply { language = queryProvider.language }

    private val _syntaxTree = MutableStateFlow<SyntaxTree?>(null)

    override val syntax = _syntaxTree.filter { syntaxTree ->
        syntaxTree?.revision == syntaxTree?.syntax?.revision
    }.map { syntaxTree ->
        syntaxTree?.syntax
    }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = null)

    private suspend inline fun <T> withTreeLock(block: () -> T) = lock.withLock {
        block()
    }

    private suspend fun getCurrentTree() = withTreeLock {
        _syntaxTree.value?.takeUnless(SyntaxTree::isClosed)?.tree
    }

    private suspend fun updateTree(update: suspend (SyntaxTree?) -> SyntaxTree?) = withTreeLock {
        val oldTree = _syntaxTree.value

        val newTree = update(oldTree)

        if (newTree != oldTree) {
            _syntaxTree.value = newTree

            oldTree?.takeUnless(SyntaxTree::isClosed)?.takeIf { tree ->
                tree != newTree
            }?.close()
        }
    }

    override suspend fun fullParse(snapshot: TextSnapshot) = Either.catch {
        withContext(dispatcher) {
            val revision = snapshot.revision

            updateTree { syntaxTree ->
                val newTree = parser.parseStringEncoding(null, snapshot.text, encoding)

                symbolIndexer.reindex(
                    tree = newTree, query = queryProvider.localsQuery, snapshot = snapshot, dirtyRange = null
                ).getOrElse { throwable ->
                    newTree.close()

                    throw throwable
                }

                SyntaxTree(
                    revision = revision, tree = newTree, syntax = Syntax(revision = revision, text = snapshot.text)
                )
            }
        }
    }

    override suspend fun applyChange(snapshot: TextSnapshot, data: TextEdit.Data, range: TextRange) = Either.catch {
        withContext(dispatcher) {
            val oldTree = getCurrentTree() ?: return@withContext

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

            val oldTreeCopy = oldTree.copy()

            oldTreeCopy.edit(inputEdit)

            val newTree = checkNotNull(parser.parseStringEncoding(oldTreeCopy, snapshot.text, encoding)) {
                "Failed to parse Haskell syntax tree"
            }

            try {
                val dirtyRange = TextRange(start = data.startPosition, end = data.newEndPosition)

                symbolIndexer.reindex(
                    tree = newTree, query = queryProvider.localsQuery, snapshot = snapshot, dirtyRange = dirtyRange
                ).getOrElse { throwable ->
                    throw throwable
                }

                updateTree { currentSyntaxTree ->
                    currentSyntaxTree?.copy(
                        revision = revision, tree = newTree, syntax = currentSyntaxTree.syntax.copy(revision = revision)
                    )
                }
            } catch (throwable: Throwable) {
                newTree.close()

                throw throwable
            } finally {
                oldTreeCopy.close()
            }
        }
    }

    override suspend fun parseFoldingRegions(snapshot: TextSnapshot, range: TextRange) = either {
        val currentTree = getCurrentTree() ?: return@either

        val currentSyntaxTree = withTreeLock { _syntaxTree.value } ?: return@either

        withContext(dispatcher) {
            val foldingRegions = foldingProvider.getSyntaxFoldingRegions(
                tree = currentTree, localsQuery = queryProvider.localsQuery, snapshot = snapshot, range = range
            ).bind()

            updateTree { latestSyntaxTree ->
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
        val currentSyntaxTree = withTreeLock {
            _syntaxTree.value?.takeUnless(SyntaxTree::isClosed)
        } ?: return@either

        withContext(dispatcher) {
            val occurrences = occurrenceProvider.getSyntaxOccurrences(position = position).bind()

            updateTree { latestSyntaxTree ->
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
        val currentTree = getCurrentTree() ?: return@either

        val currentSyntaxTree = withTreeLock { _syntaxTree.value } ?: return@either

        withContext(dispatcher) {
            val tokensPerLine = syntaxTokenProvider.getSyntaxTokensPerLine(
                tree = currentTree,
                highlightsQuery = queryProvider.highlightsQuery,
                localsQuery = queryProvider.localsQuery,
                lineLengths = (range.start.line..range.end.line).associateWith(snapshot::getLineLength),
                snapshot = snapshot,
                range = range
            ).bind()

            updateTree { latestSyntaxTree ->
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

        runBlocking { // todo
            withTreeLock {
                _syntaxTree.getAndUpdate { null }?.close()
            }
        }

        parser.close()
    }
}