package io.github.numq.haskcore.service.syntax.symbol

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.core.text.TextSnapshot
import org.treesitter.TSQuery
import org.treesitter.TSTree

internal interface SymbolIndexer {
    suspend fun reindex(
        tree: TSTree, query: TSQuery, snapshot: TextSnapshot, dirtyRange: TextRange?
    ): Either<Throwable, Unit>
}