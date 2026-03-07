package io.github.numq.haskcore.platform.splitpane

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import org.jetbrains.skiko.Cursor

@Composable
internal fun VerticalHandle(totalWidth: Float, onPositionChange: (Float) -> Unit) {
    Box(
        modifier = Modifier.fillMaxHeight().layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)

        layout(0, placeable.height) {
            placeable.placeRelative(-placeable.width / 2, 0)
        }
    }.width(8.dp).pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)))
        .pointerInput(totalWidth) {
            detectDragGestures(onDrag = { change, _ ->
                change.consume()

                onPositionChange(change.position.x)
            })
        })
}