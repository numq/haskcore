package io.github.numq.haskcore.feature.workspace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.workspace.core.WorkspaceDocument

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun WorkspaceTab(
    document: WorkspaceDocument,
    isActive: Boolean,
    select: (WorkspaceDocument) -> Unit,
    close: (WorkspaceDocument) -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    var isCloseHovered by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }

    val closeInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier.fillMaxHeight().widthIn(min = 100.dp, max = 240.dp).background(
        color = when {
            isActive -> MaterialTheme.colorScheme.surfaceVariant

            else -> Color.Transparent
        }
    ).onPointerEvent(PointerEventType.Enter) {
        isHovered = true
    }.onPointerEvent(PointerEventType.Exit) {
        isHovered = false
    }.clickable(
        enabled = !isActive,
        interactionSource = interactionSource,
        indication = null,
        onClick = { select(document) })
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = when {
                    isActive -> MaterialTheme.colorScheme.primary

                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Text(
                text = document.name,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                color = when {
                    isActive -> MaterialTheme.colorScheme.onSurface

                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                    alpha = when {
                        isActive || isHovered -> 1f

                        else -> 0f
                    }
                }.onPointerEvent(PointerEventType.Enter) {
                    isCloseHovered = true
                }.onPointerEvent(PointerEventType.Exit) {
                    isCloseHovered = false
                }.clip(CircleShape).background(
                    when {
                        isCloseHovered -> MaterialTheme.colorScheme.onSurface.copy(alpha = .1f)

                        else -> Color.Transparent
                    }
                ).clickable(
                    enabled = isActive || isHovered,
                    interactionSource = closeInteractionSource,
                    indication = null,
                    onClick = { close(document) }), contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (isActive) {
            Box(
                modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}