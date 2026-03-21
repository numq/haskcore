package io.github.numq.haskcore.feature.welcome.presentation.logo

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.platform.font.LogoFont
import io.github.numq.haskcore.platform.font.MonoFont
import kotlinx.coroutines.delay
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import kotlin.math.sin

@Composable
internal fun DistortionLogo(title: String, logoFont: LogoFont, monoFont: MonoFont, textColor: Color) {
    val transition = rememberInfiniteTransition()

    val paint = remember { Paint() }

    val glitchAmount by transition.animateFloat(
        initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3_000

                0f at 0
                0f at 2500
                1f at 2700
                1f at 3000
            }, repeatMode = RepeatMode.Restart
        )
    )

    var isStarted by remember { mutableStateOf(false) }

    val isGlitching = isStarted && glitchAmount > 0f && glitchAmount < 1f

    var isLogo by remember { mutableStateOf(false) }

    val font = when {
        isLogo -> logoFont

        else -> monoFont
    }

    LaunchedEffect(Unit) {
        delay(2_000L)

        isStarted = true
    }

    LaunchedEffect(glitchAmount == 1f) {
        if (glitchAmount == 1f) {
            isLogo = !isLogo
        }
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(128.dp)) {
        drawIntoCanvas { canvas ->
            font.createTextLine(text = title).use { textLine ->
                val xBase = (size.width - textLine.width) / 2

                val yBase = (size.height / 2) - (textLine.ascent + textLine.descent) / 2

                if (!isStarted) {
                    canvas.nativeCanvas.drawTextLine(line = textLine, x = xBase, y = yBase, paint = paint.apply {
                        color = textColor.toArgb()

                        alpha = 255
                    })

                    return@drawIntoCanvas
                }

                val slices = 12

                val textTopY = yBase + textLine.ascent

                val totalHeight = textLine.descent - textLine.ascent

                val sliceHeight = totalHeight / slices

                for (i in 0 until slices) {
                    val xOffset = when {
                        isGlitching -> sin(i.toFloat() * 1.5f) * 15f

                        else -> 0f
                    }

                    val yOffset = when {
                        isGlitching -> when (i % 2) {
                            0 -> 5f

                            else -> -5f
                        }

                        else -> 0f
                    }

                    canvas.save()

                    val clipTop = (textTopY + (i * sliceHeight)).toInt().toFloat()

                    val clipBottom = (textTopY + ((i + 1) * sliceHeight)).toInt().toFloat()

                    canvas.nativeCanvas.clipRect(
                        r = Rect.makeLTRB(l = 0f, t = clipTop, r = size.width, b = clipBottom),
                        mode = ClipMode.INTERSECT,
                        antiAlias = false
                    )

                    canvas.nativeCanvas.drawTextLine(
                        line = textLine, x = xBase + xOffset, y = yBase + yOffset, paint = paint.apply {
                            color = textColor.toArgb()

                            alpha = when {
                                isGlitching && i % 3 == 0 -> 180

                                else -> 255
                            }
                        })

                    canvas.restore()
                }
            }
        }
    }
}