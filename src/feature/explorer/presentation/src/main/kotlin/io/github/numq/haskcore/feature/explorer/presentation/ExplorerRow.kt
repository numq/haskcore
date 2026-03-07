package io.github.numq.haskcore.feature.explorer.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.explorer.core.ExplorerNode

@Composable
internal fun ExplorerRow(node: ExplorerNode, toggle: () -> Unit, select: () -> Unit, openDocument: () -> Unit) {
    val backgroundColor = when {
        node.isSelected -> MaterialTheme.colorScheme.secondaryContainer

        else -> Color.Transparent
    }

    val contentColor = when {
        node.isSelected -> MaterialTheme.colorScheme.primary

        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(24.dp).background(backgroundColor).combinedClickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null, onDoubleClick = when {
                node is ExplorerNode.Directory -> toggle

                else -> openDocument
            }, onClick = select
        ).padding(start = (node.level * 12).dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(20.dp).clickable(interactionSource = remember {
            MutableInteractionSource()
        }, indication = null, onClick = {
            if (node is ExplorerNode.Directory) {
                toggle()
            }
        }), contentAlignment = Alignment.Center) {
            if (node is ExplorerNode.Directory) {
                val rotation by animateFloatAsState(
                    when {
                        node.isExpanded -> 90f

                        else -> 0f
                    }
                )

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp).rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(2.dp))

        Icon(
            imageVector = when (node) {
                is ExplorerNode.Directory -> Icons.Default.Folder

                else -> Icons.Default.Description
            }, contentDescription = null, modifier = Modifier.size(16.dp), tint = when (node) {
                is ExplorerNode.Directory -> Color(0xFFEBCB8B)

                else -> contentColor.copy(alpha = .7f)
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = node.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = contentColor
        )
    }
}