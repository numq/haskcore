package io.github.numq.haskcore.feature.editor.presentation.background

import io.github.numq.haskcore.feature.editor.presentation.layer.Layer
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect

internal data class BackgroundLayer(val bounds: Rect, val paint: Paint) : Layer {
    override fun render(canvas: Canvas) {
        if (!paint.isClosed) {
            canvas.drawRect(r = bounds, paint = paint)
        }
    }
}