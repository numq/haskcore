package io.github.numq.haskcore.service.text.syntax

import arrow.core.Either
import arrow.core.raise.either
import io.github.numq.haskcore.core.text.TextEdit
import io.github.numq.haskcore.service.text.toTSPoint
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.treesitter.TSInputEdit
import org.treesitter.TSLanguage
import org.treesitter.TSParser
import org.treesitter.TSTree

internal class HaskellSyntaxEngine(private val haskell: TSLanguage) : SyntaxEngine {
    private val mutex = Mutex()

    private val dispatcher = Dispatchers.Default.limitedParallelism(1)

    private val parser = TSParser().apply { language = haskell }

    private val _tree = atomic<TSTree?>(null)

    private val tree get() = _tree.value

    private val _treeUpdates = MutableSharedFlow<TSTree>(replay = 1)

    override val treeUpdates = _treeUpdates.asSharedFlow()

    override suspend fun fullParse(text: String) = Either.catch {
        mutex.withLock {
            val nextTree = withContext(dispatcher) {
                parser.parseString(null, text)
            }

            _tree.value = nextTree

            _treeUpdates.emit(nextTree)
        }
    }

    override suspend fun update(text: String, edit: TextEdit) = Either.catch {
        mutex.withLock {
            val currentTree = tree

            when (currentTree) {
                null -> {
                    val initialTree = withContext(dispatcher) {
                        parser.parseString(null, text)
                    }

                    _tree.value = initialTree

                    _treeUpdates.emit(initialTree)
                }

                else -> {
                    val inputEdit = with(edit.data) {
                        TSInputEdit(
                            startByte,
                            oldEndByte,
                            newEndByte,
                            startPosition.toTSPoint(),
                            oldEndPosition.toTSPoint(),
                            newEndPosition.toTSPoint()
                        )
                    }

                    currentTree.edit(inputEdit)

                    val nextTree = withContext(dispatcher) {
                        parser.parseString(currentTree, text)
                    }

                    _tree.value = nextTree

                    _treeUpdates.emit(nextTree)
                }
            }
        }
    }

    override suspend fun <T> readTree(block: suspend (TSTree) -> T) = either {
        val currentTree = mutex.withLock {
            tree ?: raise(IllegalStateException("Tree is not initialized yet"))
        }

        Either.catch {
            withContext(dispatcher) { block(currentTree) }
        }.bind()
    }
}