package io.github.numq.haskcore.toolbar.presentation

import io.github.numq.haskcore.feature.Event
import io.github.numq.haskcore.feature.Reducer
import io.github.numq.haskcore.workspace.usecase.CloseWorkspace
import io.github.numq.haskcore.workspace.usecase.ObserveRecentWorkspaces
import io.github.numq.haskcore.workspace.usecase.ObserveWorkspace
import io.github.numq.haskcore.workspace.usecase.OpenWorkspace

internal class ToolbarReducer(
    private val observeRecentWorkspaces: ObserveRecentWorkspaces,
    private val observeWorkspace: ObserveWorkspace,
    private val openWorkspace: OpenWorkspace,
    private val closeWorkspace: CloseWorkspace,
) : Reducer<ToolbarCommand, ToolbarState> {
    override suspend fun reduce(state: ToolbarState, command: ToolbarCommand) = when (command) {
        is ToolbarCommand.Initialize -> runCatching {
            transition(
                state,
                ToolbarEvent.ObserveRecentWorkspaces(flow = observeRecentWorkspaces.execute(input = Unit).getOrThrow()),
                ToolbarEvent.ObserveWorkspace(flow = observeWorkspace.execute(input = Unit).getOrThrow())
            )
        }.getOrElse { throwable ->
            transition(state, Event.Failure(throwable = throwable))
        }

        is ToolbarCommand.UpdateRecentWorkspaces -> transition(state.copy(recentWorkspaces = command.recentWorkspaces))

        is ToolbarCommand.UpdateWorkspace -> transition(state.copy(activeWorkspace = command.workspace))

        is ToolbarCommand.OpenWorkspace -> openWorkspace.execute(input = OpenWorkspace.Input(path = command.path))
            .fold(onSuccess = {
                transition(state)
            }, onFailure = { throwable ->
                transition(state, Event.Failure(throwable = throwable))
            })

        is ToolbarCommand.CloseWorkspace -> closeWorkspace.execute(input = Unit).fold(onSuccess = {
            transition(state)
        }, onFailure = { throwable ->
            transition(state, Event.Failure(throwable = throwable))
        })

        is ToolbarCommand.ExpandWorkspaceMenu -> transition(state.copy(workspaceMenuExpanded = true))

        is ToolbarCommand.CollapseWorkspaceMenu -> transition(state.copy(workspaceMenuExpanded = false))

        is ToolbarCommand.MinimizeWindow -> transition(state, ToolbarEvent.MinimizeWindowRequested)

        is ToolbarCommand.ToggleFullscreen -> transition(state, ToolbarEvent.ToggleFullscreenRequested)

        is ToolbarCommand.ExitApplication -> transition(state, ToolbarEvent.ExitApplicationRequested)
    }
}