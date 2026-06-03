package io.github.numq.haskcore.service.syntax

import io.github.numq.haskcore.common.core.text.TextPosition
import org.treesitter.TSPoint

internal fun TSPoint.toTextPosition(): TextPosition = TextPosition(row, column)

internal fun TextPosition.toTSPoint() = TSPoint(line, column)