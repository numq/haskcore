package io.github.numq.haskcore.feature.explorer.presentation.node

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.explorer.core.ExplorerNode

@Composable
internal fun ExplorerNodeItem(
    node: ExplorerNode,
    isSelected: Boolean,
    toggleDirectoryExpansion: (ExplorerNode.Directory) -> Unit,
    select: (ExplorerNode) -> Unit,
    openDocument: (ExplorerNode) -> Unit,
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.secondaryContainer

        else -> Color.Transparent
    }

    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.primary

        else -> MaterialTheme.colorScheme.onSurface
    }

    val tapHandler = remember {
        object {
            private var lastTapNanos = 0L

            private val doubleTapGapNanos = 300_000_000L

            fun onTap(node: ExplorerNode) {
                val now = System.nanoTime()

                when {
                    now - lastTapNanos < doubleTapGapNanos -> when (node) {
                        is ExplorerNode.Directory -> toggleDirectoryExpansion(node)

                        else -> openDocument(node)
                    }

                    else -> select(node)
                }

                lastTapNanos = now
            }
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(4.dp)).background(backgroundColor)
            .pointerInput(node) {
                detectTapGestures(onTap = {
                    tapHandler.onTap(node = node)
                })
            }.padding(start = (node.level * 12f).dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(20.dp).clickable(
                interactionSource = interactionSource, indication = null, onClick = {
                    if (node is ExplorerNode.Directory) {
                        toggleDirectoryExpansion(node)
                    }
                }), contentAlignment = Alignment.Center
        ) {
            if (node is ExplorerNode.Directory) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp).rotate(
                        degrees = when {
                            node.isExpanded -> 90f

                            else -> 0f
                        }
                    ),
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