package io.github.numq.haskcore.feature.editor.presentation.menu

import io.github.numq.haskcore.common.presentation.feature.Reducer
import io.github.numq.haskcore.common.presentation.feature.action
import io.github.numq.haskcore.common.presentation.feature.effect
import io.github.numq.haskcore.feature.editor.core.usecase.CopySelection
import io.github.numq.haskcore.feature.editor.core.usecase.CutSelection
import io.github.numq.haskcore.feature.editor.core.usecase.PasteFromClipboard
import io.github.numq.haskcore.feature.editor.core.usecase.SelectAll
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorCommand
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorEvent
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorState

internal class MenuReducer(
    private val cutSelection: CutSelection,
    private val copySelection: CopySelection,
    private val pasteFromClipboard: PasteFromClipboard,
    private val selectAll: SelectAll,
) : Reducer<EditorState.Ready, EditorCommand, EditorEvent> {
    override fun reduce(state: EditorState.Ready, command: EditorCommand) = when (command) {
        is EditorCommand.Menu.Open -> with(command) {
            transition(state.copy(contextMenuState = ContextMenuState.Visible(x = x, y = y)))
        }

        is EditorCommand.Menu.Close -> transition(state.copy(contextMenuState = ContextMenuState.Hidden))

        is EditorCommand.Menu.Action.Cut -> transition(state.copy(contextMenuState = ContextMenuState.Hidden)).effect(
            action(key = command.key, fallback = EditorCommand::HandleFailure) {
                cutSelection().fold(
                    ifLeft = EditorCommand::HandleFailure, ifRight = { EditorCommand.Menu.Close })
            })

        is EditorCommand.Menu.Action.Copy -> transition(state.copy(contextMenuState = ContextMenuState.Hidden)).effect(
            action(key = command.key, fallback = EditorCommand::HandleFailure) {
                copySelection().fold(
                    ifLeft = EditorCommand::HandleFailure, ifRight = { EditorCommand.Menu.Close })
            })

        is EditorCommand.Menu.Action.Paste -> transition(state.copy(contextMenuState = ContextMenuState.Hidden)).effect(
            action(key = command.key, fallback = EditorCommand::HandleFailure) {
                pasteFromClipboard().fold(
                    ifLeft = EditorCommand::HandleFailure, ifRight = { EditorCommand.Menu.Close })
            })

        is EditorCommand.Menu.Action.SelectAll -> transition(state.copy(contextMenuState = ContextMenuState.Hidden)).effect(
            action(key = command.key, fallback = EditorCommand::HandleFailure) {
                selectAll().fold(
                    ifLeft = EditorCommand::HandleFailure, ifRight = { EditorCommand.Menu.Close })
            })

        else -> transition(state)
    }
}