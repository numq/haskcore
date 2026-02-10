package io.github.numq.haskcore.platform.window

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState

object WindowFactory {
    @Composable
    fun createWindow(
        title: String, windowState: WindowState, onCloseRequest: () -> Unit, content: @Composable () -> Unit
    ) = Window(
        onCloseRequest = onCloseRequest,
        state = windowState,
        title = title,
        undecorated = true,
        transparent = true,
        resizable = false
    ) {
        WindowDraggableArea(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }

    @Composable
    fun createDecoratedWindow(
        title: String,
        windowState: WindowState,
        onCloseRequest: () -> Unit,
        titleContent: @Composable RowScope.(WindowDecorationColors) -> Unit,
        content: @Composable () -> Unit
    ) = WindowDecoration(
        title = title,
        windowState = windowState,
        onCloseRequest = onCloseRequest,
        titleContent = titleContent,
        content = { content() })
}