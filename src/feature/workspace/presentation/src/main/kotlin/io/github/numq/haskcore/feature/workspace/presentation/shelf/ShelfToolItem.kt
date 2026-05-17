package io.github.numq.haskcore.feature.workspace.presentation.shelf

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.workspace.core.ShelfTool

@Composable
internal fun ShelfToolItem(tool: ShelfTool, isActive: Boolean, select: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }

    val isHovered by interactionSource.collectIsHoveredAsState()

    val containerColor by animateColorAsState(
        targetValue = when {
            isActive && isHovered -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = .9f)

            isActive -> MaterialTheme.colorScheme.primaryContainer

            isHovered -> MaterialTheme.colorScheme.surfaceVariant

            else -> Color.Transparent
        }
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isActive -> MaterialTheme.colorScheme.onPrimaryContainer

            isHovered -> MaterialTheme.colorScheme.onSurface

            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    )

    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.size(32.dp).hoverable(interactionSource).clickable(
                interactionSource = interactionSource, indication = null, onClick = select
            ), shape = RoundedCornerShape(8.dp), color = containerColor, contentColor = contentColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                val icon = when (tool) {
                    is ShelfTool.Explorer -> Icons.Outlined.Folder

                    is ShelfTool.Log -> Icons.Outlined.Info
                }

                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}