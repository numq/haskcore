package io.github.numq.haskcore.feature.editor.presentation.viewport

import io.github.numq.haskcore.common.core.text.TextSnapshot
import kotlin.math.ceil
import kotlin.math.floor

internal object ViewportCalculator {
    fun calculate(
        snapshot: TextSnapshot,
        visibleLineIndices: List<Int>,
        width: Float,
        height: Float,
        scrollY: Float,
        ascent: Float,
        textHeight: Float,
        lineHeight: Float,
    ) = when {
        width <= 0 || height <= 0 || lineHeight <= 0 -> Viewport.EMPTY

        else -> {
            val snappedLineHeight = ceil(lineHeight)

            val totalVisualLines = visibleLineIndices.size.coerceAtLeast(1)

            val effectiveScrollY = maxOf(0f, scrollY)

            val startVisualLine =
                floor(effectiveScrollY / snappedLineHeight).toInt().coerceIn(0, (totalVisualLines - 1).coerceAtLeast(0))

            val visualLinesInViewport = ceil(height / snappedLineHeight).toInt()

            val endVisualLine = (startVisualLine + visualLinesInViewport + 1).coerceAtMost(totalVisualLines - 1)

            val visibleVisualLinesRange = startVisualLine..endVisualLine

            val viewportLines = visibleVisualLinesRange.map { visualLineIndex ->
                val documentLineIndex = visibleLineIndices.getOrElse(visualLineIndex) { visualLineIndex }

                val text = snapshot.getLineText(line = documentLineIndex)

                val lineTop = floor((visualLineIndex * snappedLineHeight) - effectiveScrollY)

                val leading = snappedLineHeight - textHeight

                val textBaselineY = lineTop + (leading / 2f) - ascent

                ViewportLine(
                    line = documentLineIndex,
                    x = 0f,
                    y = lineTop,
                    width = width,
                    height = snappedLineHeight,
                    text = text,
                    textBaselineY = textBaselineY
                )
            }

            val visibleLines = when {
                viewportLines.isEmpty() -> IntRange.EMPTY

                else -> viewportLines.first().line..viewportLines.last().line
            }

            Viewport(
                width = width, height = height, visibleLines = visibleLines, viewportLines = viewportLines
            )
        }
    }
}