package io.github.numq.haskcore.feature.editor.presentation.selection

import io.github.numq.haskcore.feature.editor.presentation.layer.Layer
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import kotlin.math.ceil
import kotlin.math.floor

internal data class SelectionRegionLayer(
    val left: Float, val top: Float, val right: Float, val bottom: Float, val paint: Paint
) : Layer {
    override fun render(canvas: Canvas) {
        if (!paint.isClosed) {
            val left = floor(left - .5f)

            val top = floor(top)

            val right = ceil(right + .5f)

            val bottom = ceil(bottom)

            canvas.drawRect(r = Rect.makeLTRB(l = left, t = top, r = right, b = bottom), paint = paint)
        }
    }
}