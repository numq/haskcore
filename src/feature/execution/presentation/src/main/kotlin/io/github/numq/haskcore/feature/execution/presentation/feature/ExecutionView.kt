package io.github.numq.haskcore.feature.execution.presentation.feature

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.execution.core.Execution
import io.github.numq.haskcore.feature.execution.core.LaunchTarget
import io.github.numq.haskcore.feature.execution.presentation.button.ExecutionButton
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.scope.Scope

@Composable
fun ExecutionView(projectScope: Scope, handleError: (Throwable) -> Unit) {
    val scope = rememberCoroutineScope()

    val feature = koinInject<ExecutionFeature>(scope = projectScope)

    val state by feature.state.collectAsState()

    LaunchedEffect(Unit) {
        feature.events.collect { event ->
            when (event) {
                is ExecutionEvent.HandleFailure -> handleError(event.throwable)
            }
        }
    }

    var expanded by remember { mutableStateOf(false) }

    val currentConfiguration = when (val execution = state.execution) {
        is Execution.Syncing -> "Syncing"

        is Execution.Synced.NotFound -> "Not found"

        is Execution.Synced.Found -> {
            val toolchain = when (execution.currentConfiguration.target) {
                is LaunchTarget.File -> "GHC"

                is LaunchTarget.Cabal -> "Cabal"

                is LaunchTarget.Stack -> "Stack"
            }

            val name = execution.currentConfiguration.name

            "$name [$toolchain]"
        }

        is Execution.Error -> "Error"
    }

    val isRunning = state.execution is Execution.Synced.Found.Running

    Row(
        modifier = Modifier.fillMaxHeight().padding(4.dp).clip(RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Row(
                modifier = Modifier.fillMaxHeight().clip(RoundedCornerShape(4.dp)).clickable {
                    expanded = true
                }.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentConfiguration,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            when (state.execution) {
                is Execution.Synced -> DropdownMenu(expanded = expanded, onDismissRequest = {
                    expanded = false
                }) {
                    (state.execution as? Execution.Synced.Found)?.configurations?.forEach { configuration ->
                        DropdownMenuItem(text = {
                            val toolchain = when (configuration.target) {
                                is LaunchTarget.File -> "GHC"

                                is LaunchTarget.Cabal -> "Cabal"

                                is LaunchTarget.Stack -> "Stack"
                            }

                            val text = "${configuration.name} [$toolchain]"

                            Text(text)
                        }, onClick = {
                            scope.launch {
                                feature.execute(ExecutionCommand.SelectConfiguration(configuration = configuration))

                                expanded = false
                            }
                        })
                    }
                }

                else -> Unit
            }
        }

        ExecutionButton(
            imageVector = Icons.Rounded.PlayArrow, tint = when {
                isRunning -> MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)

                else -> Color(0xFF4CAF50)
            }, enabled = state.execution is Execution.Synced.Found.Stopped, onClick = {
                scope.launch {
                    feature.execute(ExecutionCommand.RunCurrentConfiguration)
                }
            })

        ExecutionButton(
            imageVector = Icons.Rounded.Stop, tint = when {
                isRunning -> MaterialTheme.colorScheme.error

                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)
            }, enabled = isRunning, onClick = {
                scope.launch {
                    feature.execute(ExecutionCommand.StopCurrentConfiguration)
                }
            })
    }
}