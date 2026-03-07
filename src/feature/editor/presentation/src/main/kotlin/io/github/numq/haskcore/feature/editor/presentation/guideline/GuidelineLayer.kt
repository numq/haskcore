package io.github.numq.haskcore.feature.editor.presentation.guideline

import io.github.numq.haskcore.feature.editor.presentation.layer.Layer
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect

internal data class GuidelineLayer(
    val x: Float, val y: Float, val width: Float, val height: Float, val paint: Paint
) : Layer {
    override fun render(canvas: Canvas) {
        if (!paint.isClosed) {
            canvas.drawRect(r = Rect.Companion.makeXYWH(l = x, t = y, w = width, h = height), paint = paint)
        }
    }
}