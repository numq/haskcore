package io.github.numq.haskcore.platform.window

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState

object WindowFactory {
    @Composable
    fun createWindow(
        title: String, state: WindowState, onCloseRequest: () -> Unit, content: @Composable () -> Unit
    ) = Window(
        onCloseRequest = onCloseRequest,
        state = state,
        title = title,
        undecorated = true,
        transparent = true,
        resizable = false
    ) {
        WindowDraggableArea(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}