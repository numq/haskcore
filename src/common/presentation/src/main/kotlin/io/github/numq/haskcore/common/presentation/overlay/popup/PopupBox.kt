package io.github.numq.haskcore.common.presentation.overlay.popup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PopupBox(modifier: Modifier, backgroundColor: Color, borderColor: Color, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.shadow(elevation = 8.dp, shape = RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        content()
    }
}