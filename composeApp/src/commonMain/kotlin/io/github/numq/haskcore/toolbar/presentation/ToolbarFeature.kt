package io.github.numq.haskcore.toolbar.presentation

import io.github.numq.haskcore.feature.Feature
import kotlinx.coroutines.*

internal class ToolbarFeature(
    private val minimizeWindow: () -> Unit,
    private val toggleFullscreen: () -> Unit,
    private val exitApplication: () -> Unit,
    private val feature: Feature<ToolbarCommand, ToolbarState>
) : Feature<ToolbarCommand, ToolbarState> by feature {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        coroutineScope.launch {
            events.collect { event ->
                when (event) {
                    is ToolbarEvent.ObserveSession -> collect(
                        event = event, joinCancellation = false, action = { session ->
                            execute(ToolbarCommand.UpdateRecentWorkspaces(recentWorkspaces = session.recentWorkspaces))
                        })

                    is ToolbarEvent.ObserveWorkspace -> collect(
                        event = event, joinCancellation = false, action = { workspace ->
                            execute(ToolbarCommand.UpdateWorkspace(workspace = workspace))
                        })

                    is ToolbarEvent.MinimizeWindowRequested -> minimizeWindow()

                    is ToolbarEvent.ToggleFullscreenRequested -> toggleFullscreen()

                    is ToolbarEvent.ExitApplicationRequested -> exitApplication()

                    else -> {
                        // todo

                        println(event)
                    }
                }
            }
        }

        coroutineScope.launch {
            execute(ToolbarCommand.Initialize)
        }
    }

    override val invokeOnClose: (suspend () -> Unit)? get() = { coroutineScope.cancel() }
}