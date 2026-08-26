package io.github.numq.haskcore.feature.explorer.presentation.feature

import io.github.numq.haskcore.common.presentation.feature.*
import io.github.numq.haskcore.feature.explorer.core.ExplorerNode
import io.github.numq.haskcore.feature.explorer.core.usecase.*
import io.github.numq.haskcore.feature.explorer.presentation.menu.MenuReducer
import kotlinx.coroutines.flow.map

internal class ExplorerReducer(
    private val menuReducer: MenuReducer,
    private val collapseDirectory: CollapseDirectory,
    private val expandDirectory: ExpandDirectory,
    private val observeExplorerTree: ObserveExplorerTree,
    private val saveExplorerPosition: SaveExplorerPosition,
    private val openFile: OpenFile,
) : Reducer<ExplorerState, ExplorerCommand, ExplorerEvent> {
    override fun reduce(state: ExplorerState, command: ExplorerCommand) = when (command) {
        is ExplorerCommand.HandleFailure -> transition(state).event(
            ExplorerEvent.HandleFailure(throwable = command.throwable)
        )

        is ExplorerCommand.Menu -> when (state) {
            is ExplorerState.Loading -> transition(state)

            is ExplorerState.Ready -> menuReducer.reduce(state = state, command = command)
        }

        is ExplorerCommand.ShowDialog -> when (state) {
            is ExplorerState.Loading -> transition(state)

            is ExplorerState.Ready -> transition(state.copy(dialog = command.dialog))
        }

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

        is ExplorerCommand.UpdateExplorerTree -> when (state) {
            is ExplorerState.Loading -> transition(ExplorerState.Ready(explorerTree = command.explorerTree))

            is ExplorerState.Ready -> transition(state.copy(explorerTree = command.explorerTree))
        }

        is ExplorerCommand.ToggleExplorerNode -> transition(state).effect(
            action(
                key = command.key, fallback = ExplorerCommand::HandleFailure, block = {
                    val node = command.node

                    when {
                        command.node.isExpanded -> collapseDirectory(
                            input = CollapseDirectory.Input(
                                node = node
                            )
                        )

                        else -> expandDirectory(input = ExpandDirectory.Input(node = node))
                    }.fold(ifLeft = ExplorerCommand::HandleFailure, ifRight = {
                        ExplorerCommand.SelectExplorerNode(node = node)
                    })
                })
        )

        is ExplorerCommand.SelectExplorerNode -> when (state) {
            is ExplorerState.Loading -> transition(state)

            is ExplorerState.Ready -> transition(state.copy(selectedNodes = listOfNotNull(command.node)))
        }

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

        is ExplorerCommand.OpenPath -> when (state) {
            is ExplorerState.Loading -> transition(state)

            is ExplorerState.Ready -> when (val node = state.explorerTree.nodes.find { node ->
                node.path == command.path
            }) {
                null -> transition(state)

                is ExplorerNode.File -> transition(state).effect(
                    action(
                        key = command.key, fallback = ExplorerCommand::HandleFailure, block = {
                            openFile(input = OpenFile.Input(path = node.path)).fold(
                                ifLeft = ExplorerCommand::HandleFailure, ifRight = {
                                    ExplorerCommand.SelectExplorerNode(node = node)
                                })
                        })
                )

                is ExplorerNode.Directory -> transition(state).effect(
                    action(
                        key = command.key, fallback = ExplorerCommand::HandleFailure, block = {
                            expandDirectory(input = ExpandDirectory.Input(node = node)).fold(
                                ifLeft = ExplorerCommand::HandleFailure, ifRight = {
                                    ExplorerCommand.SelectExplorerNode(node = node)
                                })
                        })
                )
            }
        }
    }
}