package io.github.numq.haskcore.service.syntax.token

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextRange
import org.treesitter.TSQuery
import org.treesitter.TSTree

internal interface SyntaxTokenProvider {
    suspend fun getSyntaxTokensPerLine(
        tree: TSTree, highlightsQuery: TSQuery, localsQuery: TSQuery, lineLengths: Map<Int, Int>, range: TextRange
    ): Either<Throwable, Map<Int, List<SyntaxToken>>>
}