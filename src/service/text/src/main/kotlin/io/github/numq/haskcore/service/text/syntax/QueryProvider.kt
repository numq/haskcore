package io.github.numq.haskcore.service.text.syntax

import org.treesitter.TSLanguage
import org.treesitter.TSQuery

internal interface QueryProvider {
    val language: TSLanguage

    val highlightsQuery: TSQuery

    val localsQuery: TSQuery

    val injectionsQuery: TSQuery
}