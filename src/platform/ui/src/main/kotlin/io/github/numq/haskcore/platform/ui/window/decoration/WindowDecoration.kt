package io.github.numq.haskcore.platform.ui.window.decoration

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import java.awt.Dimension
import java.awt.Frame
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.event.WindowStateListener

@Composable
fun WindowDecoration(
    title: String,
    icon: Painter? = null,
    windowDecorationHeight: Dp = 32.dp,
    windowDecorationColors: WindowDecorationColors = WindowDecorationColors(),
    windowState: WindowState,
    minimumWindowSize: DpSize? = null,
    isVisible: Boolean = true,
    isTransparent: Boolean = true,
    isResizable: Boolean = true,
    isEnabled: Boolean = true,
    isFocusable: Boolean = true,
    isAlwaysOnTop: Boolean = false,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    onCloseRequest: () -> Unit,
    titleContent: @Composable RowScope.(WindowDecorationColors) -> Unit = {},
    controlsContent: @Composable RowScope.() -> Unit = {},
    windowContent: @Composable (WindowScope.(WindowDecorationState) -> Unit),
) {
    val density = LocalDensity.current

    var isMinimized by remember { mutableStateOf(false) }

    var isFullscreen by remember { mutableStateOf(false) }

    var lastWindowBounds by remember { mutableStateOf<Rectangle?>(null) }

    val state = remember(windowState.size, windowState.position, isMinimized, isFullscreen) {
        WindowDecorationState(
            size = windowState.size,
            position = DpOffset(windowState.position.x, windowState.position.y),
            isMinimized = isMinimized,
            isFullscreen = isFullscreen,
        )
    }

    Window(
        onCloseRequest = onCloseRequest,
        state = windowState,
        visible = isVisible,
        title = title,
        icon = icon,
        undecorated = true,
        transparent = isTransparent,
        resizable = isResizable,
        enabled = isEnabled,
        focusable = isFocusable,
        alwaysOnTop = isAlwaysOnTop,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
    ) {
        LaunchedEffect(minimumWindowSize) {
            minimumWindowSize?.let { size ->
                window.minimumSize = with(density) {
                    Dimension(size.width.toPx().toInt(), size.height.toPx().toInt())
                }
            }
        }

        DisposableEffect(window) {
            val listener = WindowStateListener { event ->
                isMinimized = (event.newState and Frame.ICONIFIED) != 0
            }

            window.addWindowStateListener(listener)

            onDispose { window.removeWindowStateListener(listener) }
        }

        LaunchedEffect(isFullscreen) {
            if (window.isDisplayable) {
                when {
                    isFullscreen -> {
                        lastWindowBounds = window.bounds

                        val config = window.graphicsConfiguration

                        val insets = Toolkit.getDefaultToolkit().getScreenInsets(config)

                        val bounds = config.bounds

                        val newWidth = with(density) {
                            (bounds.width - insets.left - insets.right).coerceAtLeast(
                                minimumWindowSize?.width?.toPx()?.toInt() ?: 0
                            )
                        }

                        val newHeight = with(density) {
                            (bounds.height - insets.top - insets.bottom).coerceAtLeast(
                                minimumWindowSize?.height?.toPx()?.toInt() ?: 0
                            )
                        }

                        window.setBounds(
                            bounds.x + insets.left, bounds.y + insets.top, newWidth, newHeight
                        )
                    }

                    lastWindowBounds != null -> window.bounds = lastWindowBounds as Rectangle
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(windowDecorationHeight)
                    .background(windowDecorationColors.decoration()),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WindowDraggableArea(modifier = Modifier.weight(1f).pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        isFullscreen = !isFullscreen
                    })
                }) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                        content = { titleContent(windowDecorationColors) })
                }
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    controlsContent()
                    WindowControlIcon(
                        icon = Icons.Default.Minimize,
                        tint = windowDecorationColors.minimizeButton(),
                        onClick = { window.extendedState = Frame.ICONIFIED })
                    WindowControlIcon(
                        icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        tint = windowDecorationColors.fullscreenButton(),
                        onClick = { isFullscreen = !isFullscreen })
                    WindowControlIcon(
                        icon = Icons.Default.Close,
                        tint = windowDecorationColors.closeButton(),
                        onClick = onCloseRequest
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).background(windowDecorationColors.content()),
                contentAlignment = Alignment.TopStart
            ) {
                windowContent(state)
            }
        }
    }
}