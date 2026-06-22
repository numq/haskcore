package io.github.numq.haskcore.feature.workspace.presentation.shelf

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.awt.Cursor

@Composable
internal fun ShelfPanelContentHandle(
    totalWidth: Float, currentRatio: Float, isInverted: Boolean = false, onRatioChange: (Float) -> Unit,
) {
    val currentOnRatioChange by rememberUpdatedState(onRatioChange)

    val currentRatioState by rememberUpdatedState(currentRatio)

    Box(
        modifier = Modifier.fillMaxHeight().width(4.dp)
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)))
            .pointerInput(totalWidth) {
                var startRatio = 0f

                var cumulativeDrag = 0f

                detectDragGestures(onDragStart = {
                    startRatio = currentRatioState

                    cumulativeDrag = 0f
                }, onDrag = { change, dragAmount ->
                    change.consume()

                    cumulativeDrag += dragAmount.x

                    if (totalWidth > 0f) {
                        val deltaRatio = cumulativeDrag / totalWidth

                        val newRatio = startRatio + when {
                            isInverted -> -deltaRatio

                            else -> deltaRatio
                        }

                        currentOnRatioChange(newRatio)
                    }
                })
            })
}