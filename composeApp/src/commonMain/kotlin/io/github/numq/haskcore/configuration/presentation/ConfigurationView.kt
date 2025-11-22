package io.github.numq.haskcore.configuration.presentation

import androidx.compose.foundation.layout.*
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
import io.github.numq.haskcore.configuration.presentation.dialog.ConfigurationDialog
import io.github.numq.haskcore.configuration.presentation.dialog.ConfigurationDialogView
import kotlinx.coroutines.launch

@Composable
internal fun ConfigurationView(feature: ConfigurationFeature) {
    val coroutineScope = rememberCoroutineScope()

    val state by feature.state.collectAsState()

    Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        // todo run/rerun, stop
        IconButton(onClick = {
            coroutineScope.launch {
//                feature.execute(ConfigurationCommand.RunConfiguration)
            }
        }) {
            Icon(
                imageVector = when {
                    true -> Icons.Default.PlayArrow

                    else -> Icons.Default.Stop
                }, contentDescription = null, modifier = Modifier.size(16.dp)
            )
        }

        IconButton(onClick = {
            coroutineScope.launch {
//                feature.execute(ConfigurationCommand.StopConfiguration)
            }
        }) {
            Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
        }

        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        val command = when (state.dropdownMenu) {
                            is ConfigurationDropdownMenu.Configurations -> ConfigurationCommand.ClosePopup

                            else -> ConfigurationCommand.OpenPopup(popup = ConfigurationDropdownMenu.Configurations)
                        }

                        feature.execute(command)
                    }
                }, modifier = Modifier.width(256.dp)
            ) {
                Text(text = state.selectedConfiguration?.let { configuration ->
                    configuration.name.takeIf(String::isNotBlank) ?: configuration.command
                } ?: "No configuration",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(
                expanded = state.dropdownMenu is ConfigurationDropdownMenu.Configurations,
                onDismissRequest = {
                    coroutineScope.launch {
                        feature.execute(ConfigurationCommand.ClosePopup)
                    }
                },
                modifier = Modifier.width(256.dp),
                offset = DpOffset(x = 0.dp, y = 4.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                when {
                    state.configurations.isEmpty() -> DropdownMenuItem(text = {
                        Text(
                            text = "No other configurations",
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }, onClick = {}, enabled = false)

                    else -> state.configurations.forEach { configuration ->
                        DropdownMenuItem(text = {
                            Text(
                                text = configuration.name.takeIf(String::isNotBlank) ?: configuration.command,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }, onClick = {
                            coroutineScope.launch {
                                val command = when {
                                    configuration.id == state.selectedConfiguration?.id -> ConfigurationCommand.DeselectConfiguration

                                    else -> ConfigurationCommand.SelectConfiguration(configuration = configuration)
                                }

                                feature.execute(command)
                            }
                        }, trailingIcon = {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        feature.execute(
                                            ConfigurationCommand.OpenDialog(
                                                dialog = ConfigurationDialog.EditConfiguration(
                                                    configuration = configuration
                                                )
                                            )
                                        )
                                    }
                                }, content = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                })
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        feature.execute(
                                            ConfigurationCommand.OpenDialog(
                                                dialog = ConfigurationDialog.RemoveConfiguration(
                                                    configuration = configuration
                                                )
                                            )
                                        )
                                    }
                                }, content = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                })
                            }
                        })
                    }
                }
            }
        }

        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
            IconButton(onClick = {
                coroutineScope.launch {
                    val command = when (state.dropdownMenu) {
                        is ConfigurationDropdownMenu.Actions -> ConfigurationCommand.ClosePopup

                        else -> ConfigurationCommand.OpenPopup(popup = ConfigurationDropdownMenu.Actions)
                    }

                    feature.execute(command)
                }
            }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(16.dp))
            }

            DropdownMenu(
                expanded = state.dropdownMenu is ConfigurationDropdownMenu.Actions, onDismissRequest = {
                    coroutineScope.launch {
                        feature.execute(ConfigurationCommand.ClosePopup)
                    }
                }, modifier = Modifier.width(256.dp), offset = DpOffset(x = 0.dp, y = 4.dp)
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Add configuration",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        coroutineScope.launch {
                            feature.execute(ConfigurationCommand.OpenDialog(dialog = ConfigurationDialog.AddConfiguration))
                        }
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Edit configuration",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }, onClick = {
                        state.selectedConfiguration?.let { configuration ->
                            coroutineScope.launch {
                                feature.execute(
                                    ConfigurationCommand.OpenDialog(
                                        dialog = ConfigurationDialog.EditConfiguration(
                                            configuration = configuration
                                        )
                                    )
                                )
                            }
                        }
                    }, enabled = state.selectedConfiguration != null
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Remove configuration",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }, onClick = {
                        state.selectedConfiguration?.let { configuration ->
                            coroutineScope.launch {
                                feature.execute(
                                    ConfigurationCommand.OpenDialog(
                                        dialog = ConfigurationDialog.RemoveConfiguration(
                                            configuration = configuration
                                        )
                                    )
                                )
                            }
                        }
                    }, enabled = state.selectedConfiguration != null
                )
            }
        }
    }

    state.dialog?.let { dialog ->
        ConfigurationDialogView(dialog = dialog, addConfiguration = { path, name, command ->
            coroutineScope.launch {
                feature.execute(ConfigurationCommand.AddConfiguration(path = path, name = name, command = command))
            }
        }, editConfiguration = { configuration ->
            coroutineScope.launch {
                feature.execute(ConfigurationCommand.EditConfiguration(configuration = configuration))
            }
        }, removeConfiguration = { configuration ->
            coroutineScope.launch {
                feature.execute(ConfigurationCommand.RemoveConfiguration(configuration = configuration))
            }
        }, close = {
            coroutineScope.launch {
                feature.execute(ConfigurationCommand.CloseDialog)
            }
        })
    }
}