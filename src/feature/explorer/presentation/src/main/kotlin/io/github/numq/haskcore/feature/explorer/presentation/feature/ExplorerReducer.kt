package io.github.numq.haskcore.feature.explorer.presentation.feature

import io.github.numq.haskcore.core.feature.*
import io.github.numq.haskcore.feature.explorer.core.ExplorerNode
import io.github.numq.haskcore.feature.explorer.core.ExplorerTree
import io.github.numq.haskcore.feature.explorer.core.usecase.*
import kotlinx.coroutines.flow.map

internal class ExplorerReducer(
    private val collapseDirectory: CollapseDirectory,
    private val expandDirectory: ExpandDirectory,
    private val observeExplorerTree: ObserveExplorerTree,
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
                    val node = command.node

                    when {
                        command.node.isExpanded -> collapseDirectory(input = CollapseDirectory.Input(node = node)).fold(
                            ifLeft = ExplorerCommand::HandleFailure, ifRight = {
                                ExplorerCommand.ToggleExplorerNodeSuccess
                            })

                        else -> expandDirectory(input = ExpandDirectory.Input(node = node)).fold(
                            ifLeft = ExplorerCommand::HandleFailure, ifRight = {
                                ExplorerCommand.ToggleExplorerNodeSuccess
                            })
                    }
                })
        )

        is ExplorerCommand.ToggleExplorerNodeSuccess -> transition(state)

        is ExplorerCommand.SelectExplorerNode -> transition(state.copy(selectedPath = command.path))

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

        is ExplorerCommand.OpenPath -> when (val explorerTree = state.explorerTree) {
            is ExplorerTree.Loading -> transition(state)

            is ExplorerTree.Loaded -> when (val node = explorerTree.nodes.find { node -> node.path == command.path }) {
                null -> transition(state)

                is ExplorerNode.File -> transition(state.copy(selectedPath = command.path)).effect(
                    action(
                        key = command.key, fallback = ExplorerCommand::HandleFailure, block = {
                            openFile(input = OpenFile.Input(path = command.path)).fold(
                                ifLeft = ExplorerCommand::HandleFailure, ifRight = {
                                    ExplorerCommand.SelectExplorerNode(path = command.path)
                                })
                        })
                )

                is ExplorerNode.Directory -> transition(state.copy(selectedPath = command.path)).effect(
                    action(
                        key = command.key, fallback = ExplorerCommand::HandleFailure, block = {
                            expandDirectory(input = ExpandDirectory.Input(node = node)).fold(
                                ifLeft = ExplorerCommand::HandleFailure, ifRight = {
                                    ExplorerCommand.OpenPathSuccess
                                })
                        })
                )
            }
        }

        is ExplorerCommand.OpenPathSuccess -> transition(state)
    }
}