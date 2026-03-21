package io.github.numq.haskcore.feature.editor.presentation.caret

import io.github.numq.haskcore.feature.editor.presentation.layer.Layer
import io.github.numq.haskcore.feature.editor.presentation.measurements.Measurements
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect

data class CaretLayer(val x: Float, val y: Float, val height: Float, val paint: Paint) : Layer {
    override fun render(canvas: Canvas) {
        if (!paint.isClosed) {
            val bounds = Rect.makeXYWH(l = x, t = y, w = Measurements.CARET_WIDTH, h = height)

            canvas.drawRect(r = bounds, paint = paint)
        }
    }
}