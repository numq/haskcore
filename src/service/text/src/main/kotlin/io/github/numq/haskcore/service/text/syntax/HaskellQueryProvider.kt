package io.github.numq.haskcore.service.text.syntax

import org.treesitter.TSLanguage
import org.treesitter.TSQuery
import java.net.URL

internal class HaskellQueryProvider(
    override val language: TSLanguage, highlightsURL: URL?, localsURL: URL?, injectionsURL: URL?
) : QueryProvider {
    override val highlightsQuery by lazy {
        val query = highlightsURL?.readText() ?: error("highlights.scm not found")

        TSQuery(language, query)
    }

    override val localsQuery by lazy {
        val query = localsURL?.readText() ?: error("locals.scm not found")

        TSQuery(language, query)
    }

    override val injectionsQuery by lazy {
        val query = injectionsURL?.readText() ?: error("injections.scm not found")

        TSQuery(language, query)
    }
}