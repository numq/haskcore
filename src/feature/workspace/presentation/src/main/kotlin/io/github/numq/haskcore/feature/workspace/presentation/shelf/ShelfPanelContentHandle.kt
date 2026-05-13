package io.github.numq.haskcore.feature.workspace.presentation.shelf

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.awt.Cursor

@Composable
internal fun ShelfPanelContentHandle(totalWidth: Float, onPositionChange: (Float) -> Unit) {
    var previousX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier.fillMaxHeight().width(4.dp)
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)))
            .pointerInput(totalWidth) {
                detectDragGestures(onDragStart = { offset ->
                    previousX = offset.x
                }, onDrag = { change, _ ->
                    change.consume()

                    val deltaX = change.position.x - previousX

                    previousX = change.position.x

                    onPositionChange(deltaX)
                })
            })
}