package io.github.numq.haskcore.feature.editor.presentation.gutter

import io.github.numq.haskcore.feature.editor.presentation.layer.Layer
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint

internal data class GutterSeparatorLayer(val x: Float, val y: Float, val paint: Paint) : Layer {
    override fun render(canvas: Canvas) {
        canvas.drawLine(x0 = x, y0 = 0f, x1 = x, y1 = y, paint = paint)
    }
}