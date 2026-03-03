package io.github.numq.haskcore.service.language.semantic

import io.github.numq.haskcore.core.text.TextRange

data class SemanticToken(val range: TextRange, val type: LegendType, val modifiers: Int = 0)