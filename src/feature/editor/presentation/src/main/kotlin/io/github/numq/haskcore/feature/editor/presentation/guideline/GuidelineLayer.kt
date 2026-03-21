package io.github.numq.haskcore.feature.editor.presentation.guideline

import io.github.numq.haskcore.feature.editor.presentation.layer.Layer
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect

data class GuidelineLayer(val x: Float, val height: Float, val paint: Paint) : Layer {
    override fun render(canvas: Canvas) {
        if (!paint.isClosed) {
            canvas.drawRect(r = Rect.Companion.makeXYWH(l = x, t = 0f, w = 1f, h = height), paint = paint)
        }
    }
}