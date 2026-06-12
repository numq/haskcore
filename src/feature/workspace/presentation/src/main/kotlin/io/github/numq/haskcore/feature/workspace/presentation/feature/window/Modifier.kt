package io.github.numq.haskcore.feature.workspace.presentation.feature.window

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import java.awt.Cursor
import java.awt.MouseInfo
import java.awt.Window

internal fun Modifier.resizeHandle(
    direction: ResizeDirection,
    window: Window,
    windowState: WindowState,
    density: Density,
    minWidth: Int,
    minHeight: Int,
    onResize: (x: Float, y: Float, width: Float, height: Float) -> Unit,
) = composed {
    val cursorType = when (direction) {
        ResizeDirection.TOP -> Cursor.N_RESIZE_CURSOR

        ResizeDirection.BOTTOM -> Cursor.S_RESIZE_CURSOR

        ResizeDirection.LEFT -> Cursor.W_RESIZE_CURSOR

        ResizeDirection.RIGHT -> Cursor.E_RESIZE_CURSOR

        ResizeDirection.TOP_LEFT -> Cursor.NW_RESIZE_CURSOR

        ResizeDirection.TOP_RIGHT -> Cursor.NE_RESIZE_CURSOR

        ResizeDirection.BOTTOM_LEFT -> Cursor.SW_RESIZE_CURSOR

        ResizeDirection.BOTTOM_RIGHT -> Cursor.SE_RESIZE_CURSOR
    }

    val targetCursor = remember(cursorType) { Cursor.getPredefinedCursor(cursorType) }

    pointerHoverIcon(PointerIcon(targetCursor)).pointerInput(direction) {
        awaitPointerEventScope {
            while (true) {
                awaitFirstDown()

                val startMouse = MouseInfo.getPointerInfo().location

                val startBounds = window.bounds.clone() as java.awt.Rectangle

                var currentBounds = startBounds.clone() as java.awt.Rectangle

                while (true) {
                    val event = awaitPointerEvent()

                    val change = event.changes.firstOrNull() ?: continue

                    if (!change.pressed) break

                    val nowMouse = MouseInfo.getPointerInfo().location

                    val dx = nowMouse.x - startMouse.x

                    val dy = nowMouse.y - startMouse.y

                    currentBounds = startBounds.clone() as java.awt.Rectangle

                    when (direction) {
                        ResizeDirection.RIGHT -> {
                            currentBounds.width = (startBounds.width + dx).coerceAtLeast(minWidth)
                        }

                        ResizeDirection.BOTTOM -> {
                            currentBounds.height = (startBounds.height + dy).coerceAtLeast(minHeight)
                        }

                        ResizeDirection.LEFT -> {
                            val maxDx = startBounds.width - minWidth

                            val clampedDx = dx.coerceAtMost(maxDx)

                            currentBounds.x = startBounds.x + clampedDx

                            currentBounds.width = startBounds.width - clampedDx
                        }

                        ResizeDirection.TOP -> {
                            val maxDy = startBounds.height - minHeight

                            val clampedDy = dy.coerceAtMost(maxDy)

                            currentBounds.y = startBounds.y + clampedDy

                            currentBounds.height = startBounds.height - clampedDy
                        }

                        ResizeDirection.BOTTOM_RIGHT -> {
                            currentBounds.width = (startBounds.width + dx).coerceAtLeast(minWidth)

                            currentBounds.height = (startBounds.height + dy).coerceAtLeast(minHeight)
                        }

                        ResizeDirection.BOTTOM_LEFT -> {
                            val maxDx = startBounds.width - minWidth

                            val clampedDx = dx.coerceAtMost(maxDx)

                            currentBounds.x = startBounds.x + clampedDx

                            currentBounds.width = startBounds.width - clampedDx

                            currentBounds.height = (startBounds.height + dy).coerceAtLeast(minHeight)
                        }

                        ResizeDirection.TOP_RIGHT -> {
                            currentBounds.width = (startBounds.width + dx).coerceAtLeast(minWidth)

                            val maxDy = startBounds.height - minHeight

                            val clampedDy = dy.coerceAtMost(maxDy)

                            currentBounds.y = startBounds.y + clampedDy

                            currentBounds.height = startBounds.height - clampedDy
                        }

                        ResizeDirection.TOP_LEFT -> {
                            val maxDx = startBounds.width - minWidth

                            val clampedDx = dx.coerceAtMost(maxDx)

                            currentBounds.x = startBounds.x + clampedDx

                            currentBounds.width = startBounds.width - clampedDx

                            val maxDy = startBounds.height - minHeight

                            val clampedDy = dy.coerceAtMost(maxDy)

                            currentBounds.y = startBounds.y + clampedDy

                            currentBounds.height = startBounds.height - clampedDy
                        }
                    }

                    window.bounds = currentBounds

                    with(density) {
                        windowState.position = WindowPosition(
                            x = currentBounds.x.toDp(), y = currentBounds.y.toDp()
                        )

                        windowState.size = DpSize(
                            width = currentBounds.width.toDp(), height = currentBounds.height.toDp()
                        )
                    }

                    change.consume()
                }

                onResize(
                    currentBounds.x.toFloat(),
                    currentBounds.y.toFloat(),
                    currentBounds.width.toFloat(),
                    currentBounds.height.toFloat()
                )
            }
        }
    }
}