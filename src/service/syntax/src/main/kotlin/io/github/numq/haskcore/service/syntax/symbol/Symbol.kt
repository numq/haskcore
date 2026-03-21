package io.github.numq.haskcore.service.syntax.symbol

import io.github.numq.haskcore.core.text.TextRange

internal sealed interface Symbol {
    val name: String

    val range: TextRange

    data class Definition(override val name: String, override val range: TextRange) : Symbol

    data class Reference(override val name: String, override val range: TextRange) : Symbol
}