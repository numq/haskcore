package io.github.numq.haskcore.feature.editor.presentation.gutter

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import kotlin.math.round

class GutterActionLayer(private val rect: Rect, private val image: Image) {
    fun render(canvas: Canvas) {
        if (!image.isClosed) {
            val x = round(rect.left + (rect.width - image.width) / 2f)

            val y = round(rect.top + (rect.height - image.height) / 2f)

            canvas.drawImage(image = image, left = x, top = y)
        }
    }
}