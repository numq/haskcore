package io.github.numq.haskcore.service.text.syntax

import io.github.numq.haskcore.core.text.TextRange

data class SyntaxToken(val range: TextRange, val type: SyntaxTokenType)