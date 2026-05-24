package io.github.numq.haskcore.feature.editor.presentation.issue

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import kotlin.math.max

class IssueLayer(
    private val startX: Float, private val endX: Float, private val baselineY: Float, private val paint: Paint,
) {
    fun render(canvas: Canvas) {
        if (!paint.isClosed && startX < endX) {
            val squigglePath = Path()

            val y = baselineY + 3f

            val step = 4f

            val amplitude = 1.5f

            squigglePath.moveTo(startX, y)

            val distance = endX - startX

            val stepsCount = max(1, (distance / step).toInt())

            val actualStep = distance / stepsCount

            for (i in 1..stepsCount) {
                val currentX = startX + (i * actualStep)

                val currentY = if (i % 2 != 0) y + amplitude else y - amplitude

                squigglePath.lineTo(x = currentX, y = currentY)
            }

            canvas.drawPath(squigglePath, paint)
        }
    }
}