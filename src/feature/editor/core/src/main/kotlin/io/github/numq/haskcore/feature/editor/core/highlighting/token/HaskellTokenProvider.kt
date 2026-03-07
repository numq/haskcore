package io.github.numq.haskcore.feature.editor.core.highlighting.token

import arrow.core.raise.either
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.service.text.syntax.SyntaxToken
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

// todo

internal class HaskellTokenProvider(
    private val syntaxProvider: TokenProvider, private val semanticProvider: TokenProvider
) : TokenProvider {
    private fun mergeTokens(syntaxTokens: List<SyntaxToken>, semanticTokens: List<SyntaxToken>): List<SyntaxToken> {
        if (semanticTokens.isEmpty()) return syntaxTokens

        val semanticTokensMap = semanticTokens.associateBy(SyntaxToken::range)

        val mergedTokens = syntaxTokens.map { syntaxToken ->
            semanticTokensMap[syntaxToken.range] ?: syntaxToken
        }

        val syntaxRanges = syntaxTokens.map(SyntaxToken::range).toSet()

        val onlySemanticTokens = semanticTokens.filterNot { semanticToken -> semanticToken.range in syntaxRanges }

        return (mergedTokens + onlySemanticTokens).sortedBy { token ->
            token.range.start
        }
    }

    override suspend fun getTokens(range: TextRange) = either {
        coroutineScope {
            mergeTokens(
                syntaxTokens = async { syntaxProvider.getTokens(range) }.await().bind(),
                semanticTokens = async { semanticProvider.getTokens(range) }.await().bind()
            )
        }
    }
}