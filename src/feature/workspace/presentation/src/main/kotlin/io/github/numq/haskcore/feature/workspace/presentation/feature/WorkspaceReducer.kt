package io.github.numq.haskcore.feature.workspace.presentation.feature

import io.github.numq.haskcore.core.feature.*
import io.github.numq.haskcore.feature.workspace.core.usecase.*
import kotlinx.coroutines.flow.map

internal class WorkspaceReducer(
    private val closeWorkspaceDocument: CloseWorkspaceDocument,
    private val closeWorkspace: CloseWorkspace,
    private val observeWorkspace: ObserveWorkspace,
    private val openWorkspaceDocument: OpenWorkspaceDocument,
    private val saveDimensions: SaveDimensions,
    private val saveRatio: SaveRatio,
) : Reducer<WorkspaceState, WorkspaceCommand, WorkspaceEvent> {
    override fun reduce(state: WorkspaceState, command: WorkspaceCommand) = when (command) {
        is WorkspaceCommand.HandleFailure -> transition(state).event(WorkspaceEvent.HandleFailure(throwable = command.throwable))

        is WorkspaceCommand.Initialize -> transition(state).effect(
            action(
                key = command.key, fallback = WorkspaceCommand::HandleFailure, block = {
                    observeWorkspace(input = Unit).fold(
                        ifLeft = WorkspaceCommand::HandleFailure, ifRight = WorkspaceCommand::InitializeSuccess
                    )
                })
        )

        is WorkspaceCommand.InitializeSuccess -> transition(state).effect(
            stream(
                key = command.key,
                flow = command.flow.map(WorkspaceCommand::UpdateWorkspace),
                fallback = WorkspaceCommand::HandleFailure
            )
        )

        is WorkspaceCommand.UpdateWorkspace -> transition(state.copy(workspace = command.workspace))

        is WorkspaceCommand.CloseWorkspace -> transition(state).effect(
            action(
                key = command.key, fallback = WorkspaceCommand::HandleFailure, block = {
                    closeWorkspace(input = Unit).fold(
                        ifLeft = WorkspaceCommand::HandleFailure, ifRight = { WorkspaceCommand.CloseWorkspaceSuccess })
                })
        )

        is WorkspaceCommand.CloseWorkspaceSuccess -> transition(state)

        is WorkspaceCommand.OpenTab -> transition(state).effect(
            action(
                key = command.key, fallback = WorkspaceCommand::HandleFailure, block = {
                    openWorkspaceDocument(input = OpenWorkspaceDocument.Input(path = command.path)).fold(
                        ifLeft = WorkspaceCommand::HandleFailure, ifRight = { WorkspaceCommand.OpenTabSuccess })
                })
        )

        is WorkspaceCommand.OpenTabSuccess -> transition(state)

        is WorkspaceCommand.CloseTab -> transition(state).effect(
            action(
                key = command.key, fallback = WorkspaceCommand::HandleFailure, block = {
                    closeWorkspaceDocument(input = CloseWorkspaceDocument.Input(path = command.path)).fold(
                        ifLeft = WorkspaceCommand::HandleFailure, ifRight = { WorkspaceCommand.CloseTabSuccess })
                })
        )

        is WorkspaceCommand.CloseTabSuccess -> transition(state)

        is WorkspaceCommand.SaveDimensions -> transition(state).effect(
            action(
                key = command.key, fallback = WorkspaceCommand::HandleFailure, block = {
                    val input = with(command) {
                        SaveDimensions.Input(x = x, y = y, width = width, height = height, isFullscreen = isFullscreen)
                    }

                    saveDimensions(input = input).fold(
                        ifLeft = WorkspaceCommand::HandleFailure, ifRight = { WorkspaceCommand.SaveDimensionsSuccess })
                })
        )

        is WorkspaceCommand.SaveDimensionsSuccess -> transition(state)

        is WorkspaceCommand.SaveRatio -> transition(state).effect(
            action(
                key = command.key, fallback = WorkspaceCommand::HandleFailure, block = {
                    saveRatio(input = SaveRatio.Input(ratio = command.ratio)).fold(
                        ifLeft = WorkspaceCommand::HandleFailure, ifRight = { WorkspaceCommand.SaveRatioSuccess })
                })
        )

        is WorkspaceCommand.SaveRatioSuccess -> transition(state)
    }
}