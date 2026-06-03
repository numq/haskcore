package io.github.numq.haskcore.service.syntax.folding

import arrow.core.Either
import io.github.numq.haskcore.common.core.text.TextRange
import io.github.numq.haskcore.common.core.text.TextSnapshot
import org.treesitter.TSQuery
import org.treesitter.TSTree

internal interface SyntaxFoldingProvider {
    suspend fun getSyntaxFoldingRegions(
        tree: TSTree, localsQuery: TSQuery, snapshot: TextSnapshot, range: TextRange,
    ): Either<Throwable, List<SyntaxFoldingRegion>>
}