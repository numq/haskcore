package io.github.numq.haskcore.service.syntax.tree

import io.github.numq.haskcore.core.text.TextRevision
import io.github.numq.haskcore.service.syntax.Syntax
import kotlinx.atomicfu.atomic
import org.treesitter.TSTree

internal data class SyntaxTree(val revision: TextRevision, val tree: TSTree, val syntax: Syntax) : AutoCloseable {
    private val _isClosed = atomic(false)

    val isClosed get() = _isClosed.value

    override fun close() {
        if (!_isClosed.compareAndSet(expect = false, update = true)) {
            tree.close()
        }
    }
}