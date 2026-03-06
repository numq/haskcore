package io.github.numq.haskcore.service.text

import io.github.numq.haskcore.core.text.TextPosition
import org.treesitter.TSPoint

internal fun TSPoint.toTextPosition() = TextPosition(row, column)

internal fun TextPosition.toTSPoint() = TSPoint(line, column)