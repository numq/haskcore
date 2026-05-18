package io.github.numq.haskcore.feature.editor.presentation.background

import io.github.numq.haskcore.feature.editor.presentation.layer.Layer
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint

data class BackgroundOutlineLayer(val width: Float, val paint: Paint) : Layer {
    override fun render(canvas: Canvas) {
        if (!paint.isClosed) {
            canvas.drawLine(x0 = 0f, y0 = 0f, x1 = width, y1 = 0f, paint = paint)
        }
    }
}