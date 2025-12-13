package io.github.numq.haskcore.toolbar.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import io.github.numq.haskcore.toolbar.presentation.menu.ToolbarMenu
import io.github.numq.haskcore.workspace.Workspace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.swing.JFileChooser

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun ToolbarView(
    feature: ToolbarFeature,
    windowPlacement: WindowPlacement,
    configurationContent: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val state by feature.state.collectAsState()

    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart).padding(4.dp)) {
            TextButton(onClick = {
                coroutineScope.launch {
                    val command = when (state.menu) {
                        null -> ToolbarCommand.OpenMenu(menu = ToolbarMenu.Workspace.Root)

                        else -> ToolbarCommand.CloseMenu
                    }

                    feature.execute(command)
                }
            }) {
                Text(
                    text = "Workspace",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            DropdownMenu(
                expanded = state.menu is ToolbarMenu.Workspace,
                onDismissRequest = {
                    coroutineScope.launch {
                        feature.execute(ToolbarCommand.CloseMenu)
                    }
                },
                modifier = Modifier.width(256.dp),
                offset = DpOffset(x = 0.dp, y = 4.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.Top)
                ) {
                    DropdownMenuItem(text = {
                        Text(
                            text = "Open Workspace",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }, onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val path = JFileChooser().apply {
                                dialogTitle = "Open Workspace"
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
                    })

                    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                        DropdownMenuItem(text = {
                            Text(
                                text = "Recent Workspaces", color = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = if (state.recentWorkspaces.isNotEmpty()) 1f else .38f
                                ), style = MaterialTheme.typography.bodyMedium
                            )
                        }, onClick = {
                            coroutineScope.launch {
                                val command = when (state.menu) {
                                    ToolbarMenu.Workspace.Root -> ToolbarCommand.OpenMenu(menu = ToolbarMenu.Workspace.RecentWorkspaces)

                                    ToolbarMenu.Workspace.RecentWorkspaces -> ToolbarCommand.OpenMenu(menu = ToolbarMenu.Workspace.Root)

                                    else -> ToolbarCommand.CloseMenu
                                }

                                feature.execute(command)
                            }
                        }, enabled = state.recentWorkspaces.isNotEmpty(), trailingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        })

                        DropdownMenu(
                            expanded = state.menu is ToolbarMenu.Workspace.RecentWorkspaces,
                            onDismissRequest = {
                                coroutineScope.launch {
                                    feature.execute(ToolbarCommand.OpenMenu(menu = ToolbarMenu.Workspace.Root))
                                }
                            },
                            modifier = Modifier.width(256.dp),
                            offset = DpOffset(x = 256.dp, y = (-56).dp),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            state.recentWorkspaces.forEach { recentWorkspace ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = recentWorkspace.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = if (recentWorkspace.path != (state.activeWorkspace as? Workspace.Loaded)?.path) 1f else .38f
                                            ),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        coroutineScope.launch {
                                            feature.execute(ToolbarCommand.OpenWorkspace(path = recentWorkspace.path))
                                        }
                                    },
                                    enabled = recentWorkspace.path != (state.activeWorkspace as? Workspace.Loaded)?.path
                                )
                            }
                        }
                    }

                    DropdownMenuItem(text = {
                        Text(
                            text = "Close Workspace", color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (state.activeWorkspace is Workspace.Loaded) 1f else .38f
                            ), style = MaterialTheme.typography.bodyMedium
                        )
                    }, onClick = {
                        coroutineScope.launch {
                            feature.execute(ToolbarCommand.CloseWorkspace)
                        }
                    }, enabled = state.activeWorkspace is Workspace.Loaded)
                }
            }
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f).padding(4.dp), contentAlignment = Alignment.CenterEnd) {
                configurationContent()
            }

            Box(modifier = Modifier.clickable {
                coroutineScope.launch {
                    feature.execute(ToolbarCommand.MinimizeWindow)
                }
            }.fillMaxHeight().aspectRatio(1f), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Minimize, contentDescription = null, modifier = Modifier.size(16.dp)
                )
            }

            Box(modifier = Modifier.clickable {
                coroutineScope.launch {
                    feature.execute(ToolbarCommand.ToggleFullscreen)
                }
            }.fillMaxHeight().aspectRatio(1f), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when (windowPlacement) {
                        WindowPlacement.Floating -> Icons.Default.Fullscreen

                        else -> Icons.Default.FullscreenExit
                    }, contentDescription = null, modifier = Modifier.size(16.dp)
                )
            }

            Box(modifier = Modifier.clickable {
                coroutineScope.launch {
                    feature.execute(ToolbarCommand.ExitApplication)
                }
            }.fillMaxHeight().aspectRatio(1f), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}