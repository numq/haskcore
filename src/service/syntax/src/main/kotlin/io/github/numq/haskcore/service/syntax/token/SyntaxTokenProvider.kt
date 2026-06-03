package io.github.numq.haskcore.service.syntax.token

import arrow.core.Either
import io.github.numq.haskcore.common.core.text.TextRange
import io.github.numq.haskcore.common.core.text.TextSnapshot
import org.treesitter.TSQuery
import org.treesitter.TSTree

internal interface SyntaxTokenProvider {
    suspend fun getSyntaxTokensPerLine(
        tree: TSTree,
        highlightsQuery: TSQuery,
        localsQuery: TSQuery,
        lineLengths: Map<Int, Int>,
        snapshot: TextSnapshot,
        range: TextRange,
    ): Either<Throwable, Map<Int, List<SyntaxToken>>>
}