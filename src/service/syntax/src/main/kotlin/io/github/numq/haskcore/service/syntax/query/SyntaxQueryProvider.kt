package io.github.numq.haskcore.service.syntax.query

import org.treesitter.TSLanguage
import org.treesitter.TSQuery

internal interface SyntaxQueryProvider {
    val language: TSLanguage

    val highlightsQuery: TSQuery

    val localsQuery: TSQuery

    val injectionsQuery: TSQuery
}