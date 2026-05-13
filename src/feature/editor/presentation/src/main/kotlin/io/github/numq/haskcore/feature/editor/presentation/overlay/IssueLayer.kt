package io.github.numq.haskcore.feature.editor.presentation.overlay

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path

class IssueLayer(
    private val startX: Float, private val endX: Float, private val baselineY: Float, private val paint: Paint,
) {
    fun render(canvas: Canvas) {
        if (!paint.isClosed) {
            val squigglePath = Path()

            val y = baselineY + 3f

            val step = 4f

            val amplitude = 2f

            squigglePath.moveTo(startX, y)

            val distance = endX - startX

            val stepsCount = (distance / step).toInt()

            for (i in 1..stepsCount) {
                val currentX = startX + (i * step)

                val currentY = when {
                    i % 2 != 0 -> y + amplitude

                    else -> y - amplitude
                }

                squigglePath.lineTo(x = currentX, y = currentY)
            }

            canvas.drawPath(squigglePath, paint)
        }
    }
}