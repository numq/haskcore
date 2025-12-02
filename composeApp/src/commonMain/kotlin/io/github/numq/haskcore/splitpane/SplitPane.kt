package io.github.numq.haskcore.splitpane

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import java.awt.Cursor
import javax.swing.SwingUtilities

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun WindowScope.SplitPane(
    orientation: SplitPaneOrientation,
    percentage: Float,
    onPercentageChange: (Float) -> Unit,
    modifier: Modifier,
    thickness: Dp,
    minPercentage: Float,
    maxPercentage: Float,
    enabled: Boolean,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val containerSize = when (orientation) {
            SplitPaneOrientation.HORIZONTAL -> maxWidth

            SplitPaneOrientation.VERTICAL -> maxHeight
        }

        val minOffset by remember(containerSize, minPercentage) {
            derivedStateOf {
                (containerSize * minPercentage).coerceAtLeast(0.dp)
            }
        }

        val maxOffset by remember(containerSize, maxPercentage) {
            derivedStateOf {
                (containerSize * maxPercentage).coerceAtMost(containerSize)
            }
        }

        var offset by remember {
            mutableStateOf((containerSize * percentage).coerceIn(minOffset, maxOffset))
        }

        LaunchedEffect(percentage, containerSize, minOffset, maxOffset) {
            val newOffset = (containerSize * percentage).coerceIn(minOffset, maxOffset)

            if (newOffset != offset) {
                offset = newOffset
            }
        }

        var hoverSlider by remember { mutableStateOf(false) }

        var dragSlider by remember { mutableStateOf(false) }

        val cursor by remember(hoverSlider, dragSlider) {
            derivedStateOf {
                when {
                    hoverSlider || dragSlider -> {
                        val cursorType = when (orientation) {
                            SplitPaneOrientation.HORIZONTAL -> Cursor.E_RESIZE_CURSOR

                            SplitPaneOrientation.VERTICAL -> Cursor.S_RESIZE_CURSOR
                        }

                        Cursor(cursorType)
                    }

                    else -> Cursor.getDefaultCursor()
                }
            }
        }

        DisposableEffect(cursor) {
            SwingUtilities.invokeLater {
                window.cursor = cursor
            }

            onDispose {
                SwingUtilities.invokeLater {
                    window.cursor = Cursor.getDefaultCursor()
                }
            }
        }

        if (containerSize > 0.dp) {
            Box(modifier = Modifier.composed {
                when (orientation) {
                    SplitPaneOrientation.HORIZONTAL -> width(offset).fillMaxHeight()

                    SplitPaneOrientation.VERTICAL -> height(offset).fillMaxWidth()
                }
            }) {
                first()
            }

            Box(modifier = Modifier.composed {
                when (orientation) {
                    SplitPaneOrientation.HORIZONTAL -> fillMaxSize().padding(start = offset)

                    SplitPaneOrientation.VERTICAL -> fillMaxSize().padding(top = offset)
                }
            }) {
                second()
            }

            Box(modifier = Modifier.composed {
                when (orientation) {
                    SplitPaneOrientation.HORIZONTAL -> width(thickness).fillMaxHeight()
                        .offset(x = offset - thickness / 2)

                    SplitPaneOrientation.VERTICAL -> height(thickness).fillMaxWidth().offset(y = offset - thickness / 2)
                }
            }.pointerInput(enabled, containerSize, thickness, minOffset, maxOffset) {
                if (!enabled) return@pointerInput

                detectDragGestures(
                    onDragStart = { dragSlider = true },
                    onDragEnd = { dragSlider = false },
                    onDragCancel = { dragSlider = false },
                    onDrag = { change, dragAmount ->
                        val dragDelta = when (orientation) {
                            SplitPaneOrientation.HORIZONTAL -> dragAmount.x.toDp()

                            SplitPaneOrientation.VERTICAL -> dragAmount.y.toDp()
                        }

                        val newOffset = (offset + dragDelta).coerceIn(minOffset, maxOffset)

                        if (newOffset != offset) {
                            offset = newOffset

                            if (containerSize > 0.dp) {
                                onPercentageChange(offset / containerSize)
                            }
                        }

                        change.consume()
                    })
            }.onPointerEvent(PointerEventType.Enter) {
                hoverSlider = true
            }.onPointerEvent(PointerEventType.Exit) {
                hoverSlider = false
            })
        }
    }
}