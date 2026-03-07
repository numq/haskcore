package io.github.numq.haskcore.feature.editor.presentation.gutter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import java.awt.Cursor

@Composable
internal fun GutterIconButton(
    imageVector: ImageVector, tint: Color, size: Float, x: Float, y: Float, enabled: Boolean, onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(size.dp).graphicsLayer {
            translationX = x

            translationY = y
        }.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = enabled,
            onClick = onClick
        ).pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(size.dp).align(Alignment.Center),
            tint = if (enabled) tint else tint.copy(alpha = .5f)
        )
    }
}