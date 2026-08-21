package io.github.numq.haskcore.feature.explorer.presentation.menu

import io.github.numq.haskcore.common.presentation.feature.Reducer
import io.github.numq.haskcore.common.presentation.feature.action
import io.github.numq.haskcore.common.presentation.feature.effect
import io.github.numq.haskcore.feature.explorer.core.usecase.CreateDirectoryNode
import io.github.numq.haskcore.feature.explorer.core.usecase.CreateFileNode
import io.github.numq.haskcore.feature.explorer.presentation.dialog.ExplorerDialog
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerCommand
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerEvent
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerState

internal class MenuReducer(
    private val createDirectoryNode: CreateDirectoryNode,
    private val createFileNode: CreateFileNode,
) : Reducer<ExplorerState.Ready, ExplorerCommand.Menu, ExplorerEvent> {
    override fun reduce(
        state: ExplorerState.Ready, command: ExplorerCommand.Menu,
    ) = when (command) {
        is ExplorerCommand.Menu.Open -> with(command) {
            transition(
                state.copy(
                    selectedNodes = listOfNotNull(node),
                    contextMenuState = ContextMenuState.Visible(node = node, x = x, y = y)
                )
            )
        }

        is ExplorerCommand.Menu.Close -> transition(state.copy(contextMenuState = ContextMenuState.Hidden))

        is ExplorerCommand.Menu.Action.CreateFile -> when (command) {
            is ExplorerCommand.Menu.Action.CreateFile.ShowDialog -> transition(
                state.copy(
                    dialog = ExplorerDialog.CreateFile(
                        node = command.node
                    )
                )
            )

            is ExplorerCommand.Menu.Action.CreateFile.Confirmation -> with(command) {
                transition(state).effect(
                    action(key = key, fallback = { throwable ->
                        ExplorerCommand.Menu.Action.CreateFile.Failure(throwable = throwable)
                    }, block = {
                        createFileNode(
                            input = CreateFileNode.Input(node = node, name = name)
                        ).fold(ifLeft = { throwable ->
                            ExplorerCommand.Menu.Action.CreateFile.Failure(throwable = throwable)
                        }, ifRight = {
                            ExplorerCommand.Menu.Action.CreateFile.Success
                        })
                    })
                )
            }

            is ExplorerCommand.Menu.Action.CreateFile.Success -> transition(state.copy(dialog = ExplorerDialog.None))

            is ExplorerCommand.Menu.Action.CreateFile.Failure -> when (state.dialog) {
                is ExplorerDialog.CreateFile -> transition(
                    state.copy(
                        dialog = state.dialog.copy(
                            error = command.throwable.localizedMessage.takeIf(
                                String::isNotEmpty
                            )
                        )
                    )
                )

                else -> transition(state)
            }
        }

        is ExplorerCommand.Menu.Action.CreateDirectory -> when (command) {
            is ExplorerCommand.Menu.Action.CreateDirectory.ShowDialog -> transition(
                state.copy(
                    dialog = ExplorerDialog.CreateDirectory(
                        node = command.node
                    )
                )
            )

            is ExplorerCommand.Menu.Action.CreateDirectory.Confirmation -> with(command) {
                transition(state).effect(
                    action(key = key, fallback = { throwable ->
                        ExplorerCommand.Menu.Action.CreateDirectory.Failure(throwable = throwable)
                    }, block = {
                        createDirectoryNode(
                            input = CreateDirectoryNode.Input(node = node, name = command.name)
                        ).fold(ifLeft = { throwable ->
                            ExplorerCommand.Menu.Action.CreateDirectory.Failure(throwable = throwable)
                        }, ifRight = {
                            ExplorerCommand.Menu.Action.CreateDirectory.Success
                        })
                    })
                )
            }

            is ExplorerCommand.Menu.Action.CreateDirectory.Success -> transition(state.copy(dialog = ExplorerDialog.None))

            is ExplorerCommand.Menu.Action.CreateDirectory.Failure -> when (state.dialog) {
                is ExplorerDialog.CreateDirectory -> transition(
                    state.copy(
                        dialog = state.dialog.copy(
                            error = command.throwable.localizedMessage.takeIf(
                                String::isNotEmpty
                            )
                        )
                    )
                )

                else -> transition(state)
            }
        }

        is ExplorerCommand.Menu.Action.Move -> transition(state) // todo

        is ExplorerCommand.Menu.Action.Cut -> transition(state) // todo

        is ExplorerCommand.Menu.Action.Copy -> transition(state) // todo

        is ExplorerCommand.Menu.Action.Paste -> transition(state) // todo

        is ExplorerCommand.Menu.Action.Delete -> transition(state) // todo
    }
}