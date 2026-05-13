package io.github.numq.haskcore.feature.editor.presentation.menu

import io.github.numq.haskcore.common.presentation.feature.Reducer
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorCommand
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorEvent
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorState

internal class MenuReducer : Reducer<EditorState.Ready, EditorCommand.Menu, EditorEvent> {
    override fun reduce(state: EditorState.Ready, command: EditorCommand.Menu) = when (command) {
        is EditorCommand.Menu.Open -> with(command) {
            transition(state.copy(menu = EditorMenu.Visible(x = x, y = y)))
        }

        is EditorCommand.Menu.Close -> transition(state.copy(menu = EditorMenu.Hidden))

        is EditorCommand.Menu.RunStack -> transition(state) // todo

        is EditorCommand.Menu.RunCabal -> transition(state) // todo

        is EditorCommand.Menu.RunGhc -> transition(state) // todo

        is EditorCommand.Menu.Cut -> transition(state) // todo

        is EditorCommand.Menu.Copy -> transition(state) // todo

        is EditorCommand.Menu.Paste -> transition(state) // todo

        is EditorCommand.Menu.SelectAll -> transition(state) // todo
    }
}