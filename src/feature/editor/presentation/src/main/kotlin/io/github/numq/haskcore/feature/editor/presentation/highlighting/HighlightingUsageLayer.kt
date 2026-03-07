package io.github.numq.haskcore.feature.editor.presentation.highlighting

import io.github.numq.haskcore.feature.editor.presentation.layer.Layer
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect

internal data class HighlightingUsageLayer(val rect: Rect, val paint: Paint) : Layer {
    override fun render(canvas: Canvas) {
        if (!paint.isClosed) {
            canvas.drawRect(r = rect, paint = paint)
        }
    }
}