package io.github.numq.haskcore.feature.explorer.presentation.menu

import io.github.numq.haskcore.common.presentation.feature.Reducer
import io.github.numq.haskcore.common.presentation.overlay.menu.ContextMenuState
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerCommand
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerEvent
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerState

internal class MenuReducer : Reducer<ExplorerState.Ready, ExplorerCommand.Menu, ExplorerEvent> {
    override fun reduce(state: ExplorerState.Ready, command: ExplorerCommand.Menu) = when (command) {
        is ExplorerCommand.Menu.Open -> with(command) {
            transition(state.copy(contextMenuState = ContextMenuState.Visible(x = x, y = y)))
        }

        is ExplorerCommand.Menu.Close -> transition(state.copy(contextMenuState = ContextMenuState.Hidden))

        is ExplorerCommand.Menu.Action.CreateFile -> transition(state) // todo

        is ExplorerCommand.Menu.Action.CreateDirectory -> transition(state) // todo

        is ExplorerCommand.Menu.Action.Cut -> transition(state) // todo

        is ExplorerCommand.Menu.Action.Copy -> transition(state) // todo

        is ExplorerCommand.Menu.Action.Paste -> transition(state) // todo

        is ExplorerCommand.Menu.Action.Delete -> transition(state) // todo
    }
}