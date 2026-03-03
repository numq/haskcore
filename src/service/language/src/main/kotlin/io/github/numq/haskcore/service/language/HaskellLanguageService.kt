package io.github.numq.haskcore.service.language

import arrow.core.flatMap
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.service.language.gateway.LspGateway
import io.github.numq.haskcore.service.language.semantic.SemanticDecoder

internal class HaskellLanguageService(
    private val lspGateway: LspGateway, private val semanticDecoder: SemanticDecoder
) : LanguageService {
    override suspend fun initialize() = lspGateway.initialize().map {}

    override suspend fun getSemanticTokens(
        uri: String, range: TextRange
    ) = lspGateway.requestSemanticTokens(uri).flatMap { data ->
        semanticDecoder.decode(data = data, legend = lspGateway.semanticLegend.value).map { semanticTokens ->
            semanticTokens.filter { semanticToken ->
                range.contains(semanticToken.range)
            }
        }
    }
}