package io.github.numq.haskcore.service.syntax.occurrence

import io.github.numq.haskcore.core.text.TextRange

sealed interface SyntaxOccurrence {
    val range: TextRange

    data class Definition(override val range: TextRange) : SyntaxOccurrence

    data class Reference(override val range: TextRange) : SyntaxOccurrence
}