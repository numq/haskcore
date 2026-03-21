package io.github.numq.haskcore.feature.editor.presentation.text

import io.github.numq.haskcore.feature.editor.presentation.layer.Layer
import io.github.numq.haskcore.feature.editor.presentation.viewport.ViewportLine
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.paragraph.Paragraph
import org.jetbrains.skia.paragraph.RectHeightMode
import org.jetbrains.skia.paragraph.RectWidthMode

data class TextContentLayer(
    val viewportLine: ViewportLine, val paragraph: Paragraph, val x: Float, val y: Float
) : Layer {
    fun getCoordinateAtOffset(offset: Int): Float {
        if (paragraph.isClosed || offset <= 0) return 0f

        val rects = paragraph.getRectsForRange(
            start = offset - 1, end = offset, rectHeightMode = RectHeightMode.TIGHT, rectWidthMode = RectWidthMode.TIGHT
        )

        return rects.firstOrNull()?.rect?.right ?: 0f
    }

    fun getOffsetAtCoordinate(targetX: Float): Int {
        if (paragraph.isClosed || targetX <= 0) return 0

        val localX = targetX - x

        return paragraph.getGlyphPositionAtCoordinate(localX, 0f).position
    }

    override fun render(canvas: Canvas) {
        if (!paragraph.isClosed) {
            paragraph.paint(canvas, x, y)
        }
    }
}