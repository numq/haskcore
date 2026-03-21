package io.github.numq.haskcore.feature.editor.presentation.background

import io.github.numq.haskcore.feature.editor.presentation.layer.Layer
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect

data class BackgroundLayer(val width: Float, val height: Float, val paint: Paint) : Layer {
    override fun render(canvas: Canvas) {
        if (!paint.isClosed) {
            val bounds = Rect.makeWH(w = width, h = height)

            canvas.drawRect(r = bounds, paint = paint)
        }
    }
}