package io.github.numq.haskcore.toolbar.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import io.github.numq.haskcore.configuration.presentation.ConfigurationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.swing.JFileChooser

@Composable
internal fun WindowScope.ToolbarView(feature: ToolbarFeature) {
    val coroutineScope = rememberCoroutineScope()

    val state by feature.state.collectAsState()

    Surface {
        Row(
            modifier = Modifier.combinedClickable(enabled = false, onDoubleClick = {
                coroutineScope.launch {
                    feature.execute(ToolbarCommand.ToggleFullscreen)
                }
            }, onClick = {}).fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f).wrapContentSize(Alignment.TopStart)) {
                TextButton(onClick = {
                    coroutineScope.launch {
                        val command = when {
                            state.workspaceMenuExpanded -> ToolbarCommand.CollapseWorkspaceMenu

                            else -> ToolbarCommand.ExpandWorkspaceMenu
                        }

                        feature.execute(command)
                    }
                }, modifier = Modifier.widthIn(min = 128.dp)) {
                    Text(
                        text = state.activeWorkspace?.name ?: "No workspace",
                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = state.workspaceMenuExpanded,
                    onDismissRequest = {
                        coroutineScope.launch {
                            feature.execute(ToolbarCommand.CollapseWorkspaceMenu)
                        }
                    },
                    modifier = Modifier.sizeIn(minWidth = 128.dp, maxHeight = 512.dp),
                    offset = DpOffset(x = 0.dp, y = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.Top)
                    ) {
                        if (state.activeWorkspace != null) {
                            Text(
                                text = "Close Workspace",
                                modifier = Modifier.clickable {
                                    coroutineScope.launch {
                                        feature.execute(ToolbarCommand.CloseWorkspace)
                                    }
                                }.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Text(
                            text = "Open Workspace",
                            modifier = Modifier.clickable {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val path = JFileChooser().apply {
                                        name = "Open any directory as a workspace"
                                        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                                    }.run {
                                        when {
                                            showOpenDialog(null) == JFileChooser.APPROVE_OPTION -> selectedFile.absolutePath

                                            else -> null
                                        }
                                    }

                                    if (path != null) {
                                        coroutineScope.launch {
                                            feature.execute(ToolbarCommand.OpenWorkspace(path = path))
                                        }
                                    }
                                }
                            }.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        HorizontalDivider()

                        Text(
                            text = "Recent Workspaces",
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                            style = MaterialTheme.typography.labelSmall
                        )

                        state.recentWorkspaces.filterNot { workspace ->
                            workspace.path == state.activeWorkspace?.path
                        }.forEach { workspace ->
                            Text(
                                text = workspace.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable {
                                    coroutineScope.launch {
                                        feature.execute(ToolbarCommand.OpenWorkspace(path = workspace.path))
                                    }
                                }.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConfigurationView(
                    modifier = Modifier.weight(1f).widthIn(min = 128.dp).wrapContentSize(Alignment.TopEnd),
                )

                FilledIconButton(onClick = {
                    coroutineScope.launch {
                        feature.execute(ToolbarCommand.MinimizeWindow)
                    }
                }, shape = CircleShape) {
                    Icon(
                        imageVector = Icons.Filled.Minimize, contentDescription = null, modifier = Modifier.size(16.dp)
                    )
                }

                FilledIconButton(onClick = {
                    coroutineScope.launch {
                        feature.execute(ToolbarCommand.ToggleFullscreen)
                    }
                }, shape = CircleShape) {
                    Icon(
                        imageVector = when {
                            window.isMaximumSizeSet -> Icons.Filled.FullscreenExit

                            else -> Icons.Filled.Fullscreen
                        }, contentDescription = null, modifier = Modifier.size(16.dp)
                    )
                }

                FilledIconButton(onClick = {
                    coroutineScope.launch {
                        feature.execute(ToolbarCommand.ExitApplication)
                    }
                }, shape = CircleShape) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}