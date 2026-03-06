package io.github.numq.haskcore.service.text.occurrence

import io.github.numq.haskcore.core.text.TextRange

sealed interface Occurrence {
    val range: TextRange

    data class Definition(override val range: TextRange) : Occurrence

    data class Reference(override val range: TextRange) : Occurrence

    data class Write(override val range: TextRange) : Occurrence

    data class Read(override val range: TextRange) : Occurrence
}