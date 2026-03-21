package io.github.numq.haskcore.feature.execution.presentation.button

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
internal fun ExecutionButton(
    imageVector: ImageVector, tint: Color, enabled: Boolean, onClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxHeight().aspectRatio(1f).clip(RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(4.dp), contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = imageVector, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
    }
}