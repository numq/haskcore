package io.github.numq.haskcore.feature.workspace.presentation.feature

import io.github.numq.haskcore.common.presentation.feature.*
import io.github.numq.haskcore.feature.workspace.core.usecase.*
import kotlinx.coroutines.flow.map

internal class WorkspaceReducer(
    private val closeWorkspaceDocument: CloseWorkspaceDocument,
    private val closeWorkspace: CloseWorkspace,
    private val observeWorkspace: ObserveWorkspace,
    private val openWorkspaceDocument: OpenWorkspaceDocument,
    private val saveDimensions: SaveDimensions,
    private val saveVerticalRatio: SaveVerticalRatio,
    private val selectShelfTool: SelectShelfTool,
    private val saveLeftShelfPanelRatio: SaveLeftShelfPanelRatio,
    private val saveRightShelfPanelRatio: SaveRightShelfPanelRatio,
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

        is WorkspaceCommand.UpdateWorkspace -> when (state) {
            is WorkspaceState.Loading -> transition(WorkspaceState.Ready(workspace = command.workspace))

            is WorkspaceState.Ready -> transition(state.copy(workspace = command.workspace))
        }

        is WorkspaceCommand.CloseWorkspace -> transition(state).effect(
            action(
                key = command.key, fallback = WorkspaceCommand::HandleFailure, block = {
                    closeWorkspace(input = Unit).fold(
                        ifLeft = WorkspaceCommand::HandleFailure, ifRight = { WorkspaceCommand.CloseWorkspaceSuccess })
                })
        )

        is WorkspaceCommand.CloseWorkspaceSuccess -> transition(state)

        is WorkspaceCommand.OpenDocument -> transition(state).effect(
            action(
                key = command.key, fallback = WorkspaceCommand::HandleFailure, block = {
                    openWorkspaceDocument(input = OpenWorkspaceDocument.Input(path = command.document.path)).fold(
                        ifLeft = WorkspaceCommand::HandleFailure, ifRight = { WorkspaceCommand.OpenDocumentSuccess })
                })
        )

        is WorkspaceCommand.OpenDocumentSuccess -> transition(state)

        is WorkspaceCommand.CloseDocument -> transition(state).effect(
            action(
                key = command.key, fallback = WorkspaceCommand::HandleFailure, block = {
                    closeWorkspaceDocument(input = CloseWorkspaceDocument.Input(path = command.document.path)).fold(
                        ifLeft = WorkspaceCommand::HandleFailure, ifRight = { WorkspaceCommand.CloseDocumentSuccess })
                })
        )

        is WorkspaceCommand.CloseDocumentSuccess -> transition(state)

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

        is WorkspaceCommand.SaveVerticalRatio -> transition(state).effect(
            action(
                key = command.key, fallback = WorkspaceCommand::HandleFailure, block = {
                    saveVerticalRatio(input = SaveVerticalRatio.Input(ratio = command.ratio)).fold(
                        ifLeft = WorkspaceCommand::HandleFailure,
                        ifRight = { WorkspaceCommand.SaveVerticalRatioSuccess })
                })
        )

        is WorkspaceCommand.SaveVerticalRatioSuccess -> transition(state)

        is WorkspaceCommand.SelectShelfTool -> transition(state).effect(
            action(
                key = command.key, fallback = WorkspaceCommand::HandleFailure, block = {
                    selectShelfTool(input = SelectShelfTool.Input(tool = command.tool)).fold(
                        ifLeft = WorkspaceCommand::HandleFailure, ifRight = { WorkspaceCommand.SelectShelfToolSuccess })
                })
        )

        is WorkspaceCommand.SelectShelfToolSuccess -> transition(state)

        is WorkspaceCommand.SaveLeftShelfPanelRatio -> transition(state).effect(
            action(
                key = command.key, fallback = WorkspaceCommand::HandleFailure, block = {
                    saveLeftShelfPanelRatio(input = SaveLeftShelfPanelRatio.Input(ratio = command.ratio)).fold(
                        ifLeft = WorkspaceCommand::HandleFailure,
                        ifRight = { WorkspaceCommand.SaveLeftShelfPanelRatioSuccess })
                })
        )

        is WorkspaceCommand.SaveLeftShelfPanelRatioSuccess -> transition(state)

        is WorkspaceCommand.SaveRightShelfPanelRatio -> transition(state).effect(
            action(
                key = command.key, fallback = WorkspaceCommand::HandleFailure, block = {
                    saveRightShelfPanelRatio(input = SaveRightShelfPanelRatio.Input(ratio = command.ratio)).fold(
                        ifLeft = WorkspaceCommand::HandleFailure,
                        ifRight = { WorkspaceCommand.SaveRightShelfPanelRatioSuccess })
                })
        )

        is WorkspaceCommand.SaveRightShelfPanelRatioSuccess -> transition(state)
    }
}