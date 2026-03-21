package io.github.numq.haskcore.feature.editor.core.syntax

import io.github.numq.haskcore.core.text.TextRange

sealed interface Occurrence {
    val range: TextRange

    data class Definition(override val range: TextRange) : Occurrence

    data class Reference(override val range: TextRange) : Occurrence
}