package io.github.numq.haskcore.service.syntax.folding

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextRange
import org.treesitter.TSQuery
import org.treesitter.TSTree

internal interface SyntaxFoldingProvider {
    suspend fun getSyntaxFoldingRegions(
        tree: TSTree, localsQuery: TSQuery, range: TextRange
    ): Either<Throwable, List<SyntaxFoldingRegion>>
}