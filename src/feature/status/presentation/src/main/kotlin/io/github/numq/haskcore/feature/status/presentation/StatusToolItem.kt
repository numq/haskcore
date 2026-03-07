package io.github.numq.haskcore.feature.status.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.status.core.StatusTool

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun StatusToolItem(name: String, tool: StatusTool, selectPath: () -> Unit, resetPath: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val tint by animateColorAsState(
        when (tool) {
            is StatusTool.Ready -> Color(0xFF4CAF50)

            is StatusTool.Error -> MaterialTheme.colorScheme.error

            is StatusTool.NotFound -> MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)

            is StatusTool.Scanning -> MaterialTheme.colorScheme.primary
        }
    )

    val tooltipText = when (tool) {
        is StatusTool.Ready -> "Version: ${tool.version}\nPath: ${tool.path}"

        is StatusTool.Error -> {
            val message = tool.throwable.message ?: "Unknown error"

            val shortMessage = message.lines().firstOrNull(String::isNotBlank)?.take(100) ?: message

            "Error: $shortMessage${if (message.length > shortMessage.length) "..." else ""}"
        }

        is StatusTool.NotFound -> "Not found. Click to configure."

        is StatusTool.Scanning -> "Scanning..."
    }

    TooltipArea(
        delayMillis = 500, tooltip = {
            Surface(shape = MaterialTheme.shapes.extraSmall, tonalElevation = 4.dp) {
                Text(
                    text = tooltipText,
                    modifier = Modifier.padding(8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }) {
        Box {
            Row(
                modifier = Modifier.fillMaxHeight().clickable(enabled = tool !is StatusTool.Scanning, onClick = {
                    expanded = true
                }).padding(start = 8.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    modifier = Modifier.fillMaxHeight(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge
                )

                Box(modifier = Modifier.size(14.dp), contentAlignment = Alignment.Center) {
                    when (tool) {
                        is StatusTool.Scanning -> CircularProgressIndicator(
                            modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = tint
                        )

                        else -> Icon(
                            imageVector = when (tool) {
                                is StatusTool.Error -> Icons.Default.Error

                                is StatusTool.Ready -> Icons.Default.CheckCircle

                                else -> Icons.Default.AddCircle
                            }, contentDescription = null, modifier = Modifier.fillMaxSize(), tint = tint
                        )
                    }
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = {
                expanded = false
            }) {
                DropdownMenuItem(text = {
                    Text(
                        text = "Select custom path",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }, onClick = {
                    expanded = false

                    selectPath()
                }, leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                })
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Reset to default", style = MaterialTheme.typography.labelMedium
                        )
                    }, onClick = {
                        expanded = false

                        resetPath()
                    }, leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }, colors = MenuItemColors(
                        textColor = MaterialTheme.colorScheme.onSurface,
                        leadingIconColor = MaterialTheme.colorScheme.secondary,
                        trailingIconColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .38f),
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .38f),
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)
                    )
                )
            }
        }
    }
}