package io.github.numq.haskcore.feature.editor.presentation.viewport

import io.github.numq.haskcore.core.text.TextSnapshot
import kotlin.math.ceil
import kotlin.math.floor

internal object ViewportCalculator {
    fun calculate(
        snapshot: TextSnapshot,
        width: Float,
        height: Float,
        scrollY: Float,
        ascent: Float,
        textHeight: Float,
        lineHeight: Float
    ) = when {
        width <= 0 || height <= 0 || lineHeight <= 0 -> Viewport.EMPTY

        else -> {
            val totalLines = snapshot.lines.coerceAtLeast(1)

            val effectiveScrollY = maxOf(0f, scrollY)

            val startLine = floor(effectiveScrollY / lineHeight).toInt().coerceIn(0, (totalLines - 1).coerceAtLeast(0))

            val linesInViewport = ceil(height / lineHeight).toInt()

            val endLine = (startLine + linesInViewport + 1).coerceAtMost(totalLines - 1)

            val visibleLinesRange = startLine..endLine

            val viewportLines = visibleLinesRange.map { lineIndex ->
                val text = snapshot.getLineText(line = lineIndex)

                val lineTop = (lineIndex * lineHeight) - scrollY

                val leading = lineHeight - textHeight

                val textBaselineY = lineTop + (leading / 2) - ascent

                ViewportLine(
                    line = lineIndex,
                    x = 0f,
                    y = lineTop,
                    width = width,
                    height = lineHeight,
                    text = text,
                    textBaselineY = textBaselineY
                )
            }

            Viewport(width = width, height = height, visibleLines = visibleLinesRange, viewportLines = viewportLines)
        }
    }
}