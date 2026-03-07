package io.github.numq.haskcore.feature.explorer.presentation

import io.github.numq.haskcore.core.feature.*
import io.github.numq.haskcore.feature.explorer.core.usecase.*
import kotlinx.coroutines.flow.map

internal class ExplorerReducer(
    private val observeExplorerTree: ObserveExplorerTree,
    private val selectExplorerNode: SelectExplorerNode,
    private val toggleExplorerNode: ToggleExplorerNode,
    private val saveExplorerPosition: SaveExplorerPosition,
    private val openFile: OpenFile,
) : Reducer<ExplorerState, ExplorerCommand, ExplorerEvent> {
    override fun reduce(
        state: ExplorerState, command: ExplorerCommand
    ): Transition<ExplorerState, ExplorerEvent> = when (command) {
        is ExplorerCommand.HandleFailure -> transition(state).event(ExplorerEvent.HandleFailure(throwable = command.throwable))

        is ExplorerCommand.Initialize -> transition(state).effect(
            action(
                key = command.key, fallback = ExplorerCommand::HandleFailure, block = {
                    observeExplorerTree(input = Unit).fold(
                        ifLeft = ExplorerCommand::HandleFailure, ifRight = ExplorerCommand::InitializeSuccess
                    )
                })
        )

        is ExplorerCommand.InitializeSuccess -> transition(state).effect(
            stream(
                key = command.key,
                flow = command.flow.map(ExplorerCommand::UpdateExplorerTree),
                fallback = ExplorerCommand::HandleFailure
            )
        )

        is ExplorerCommand.UpdateExplorerTree -> transition(state.copy(explorerTree = command.explorerTree))

        is ExplorerCommand.ToggleExplorerNode -> transition(state).effect(
            action(
                key = command.key, fallback = ExplorerCommand::HandleFailure, block = {
                    toggleExplorerNode(input = ToggleExplorerNode.Input(path = command.path)).fold(
                        ifLeft = ExplorerCommand::HandleFailure, ifRight = {
                            ExplorerCommand.ToggleExplorerNodeSuccess
                        })
                })
        )

        is ExplorerCommand.ToggleExplorerNodeSuccess -> transition(state)

        is ExplorerCommand.SelectExplorerNode -> transition(state).effect(
            action(
                key = command.key, fallback = ExplorerCommand::HandleFailure, block = {
                    selectExplorerNode(input = SelectExplorerNode.Input(path = command.path)).fold(
                        ifLeft = ExplorerCommand::HandleFailure, ifRight = {
                            ExplorerCommand.SelectExplorerNodeSuccess
                        })
                })
        )

        is ExplorerCommand.SelectExplorerNodeSuccess -> transition(state)

        is ExplorerCommand.SaveExplorerPosition -> transition(state).effect(
            action(
                key = command.key, fallback = ExplorerCommand::HandleFailure, block = {
                    saveExplorerPosition(
                        input = SaveExplorerPosition.Input(position = command.position)
                    ).fold(ifLeft = ExplorerCommand::HandleFailure, ifRight = {
                        ExplorerCommand.SaveExplorerPositionSuccess
                    })
                })
        )

        is ExplorerCommand.SaveExplorerPositionSuccess -> transition(state)

        is ExplorerCommand.OpenFile -> transition(state).effect(
            action(
                key = command.key, fallback = ExplorerCommand::HandleFailure, block = {
                    openFile(input = OpenFile.Input(path = command.path)).fold(
                        ifLeft = ExplorerCommand::HandleFailure, ifRight = {
                            ExplorerCommand.SelectExplorerNode(path = command.path)
                        })
                })
        )
    }
}