package io.github.numq.haskcore.feature.editor.presentation.guideline

import io.github.numq.haskcore.feature.editor.presentation.layer.Layer
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint

data class GuidelineLayer(val x: Float, val height: Float, val paint: Paint) : Layer {
    override fun render(canvas: Canvas) {
        if (!paint.isClosed) {
            canvas.drawLine(x0 = x, y0 = 0f, x1 = x, y1 = height, paint = paint)
        }
    }
}