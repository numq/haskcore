package io.github.numq.haskcore.feature.editor.presentation.feature

import io.github.numq.haskcore.core.feature.*
import io.github.numq.haskcore.feature.editor.core.usecase.*
import kotlinx.coroutines.flow.map

internal class EditorReducer(
    private val observeEditor: ObserveEditor,
    private val updateActiveLines: UpdateActiveLines,
    private val processKey: ProcessKey,
    private val moveCaret: MoveCaret,
    private val startSelection: StartSelection,
    private val extendSelection: ExtendSelection,
) : Reducer<EditorState, EditorCommand, EditorEvent> {
    override fun reduce(state: EditorState, command: EditorCommand) = when (command) {
        is EditorCommand.HandleFailure -> transition(state).event(EditorEvent.HandleFailure(throwable = command.throwable))

        is EditorCommand.Initialize -> transition(state).effect(
            action(
                key = command.key, fallback = EditorCommand::HandleFailure, block = {
                    observeEditor(input = Unit).fold(
                        ifLeft = EditorCommand::HandleFailure, ifRight = EditorCommand::InitializeSuccess
                    )
                })
        )

        is EditorCommand.InitializeSuccess -> transition(state).effect(
            stream(
                key = command.key,
                flow = command.flow.map(EditorCommand::UpdateEditor),
                fallback = EditorCommand::HandleFailure
            )
        )

        is EditorCommand.UpdateEditor -> when (state) {
            is EditorState.Loading -> transition(EditorState.Ready(editor = command.editor))

            is EditorState.Ready -> transition(state.copy(editor = command.editor))
        }

        is EditorCommand.UpdateViewport -> transition(state).effect(
            action(
                key = command.key, fallback = EditorCommand::HandleFailure, block = {
                    updateActiveLines.invoke(
                        input = UpdateActiveLines.Input(start = command.start, end = command.end)
                    ).fold(ifLeft = EditorCommand::HandleFailure, ifRight = {
                        EditorCommand.UpdateViewportSuccess
                    })
                })
        )

        is EditorCommand.UpdateViewportSuccess -> transition(state)

        is EditorCommand.ProcessKey -> transition(state).effect(
            action(
                key = command.key, fallback = EditorCommand::HandleFailure, block = {
                    processKey(
                        input = ProcessKey.Input(
                            keyCode = command.keyCode,
                            modifiers = command.modifiers,
                            utf16CodePoint = command.utf16CodePoint
                        )
                    ).fold(
                        ifLeft = EditorCommand::HandleFailure, ifRight = {
                            EditorCommand.ProcessKeySuccess
                        })
                })
        )

        is EditorCommand.ProcessKeySuccess -> transition(state)

        is EditorCommand.MoveCaret -> transition(state).effect(
            action(
                key = command.key, fallback = EditorCommand::HandleFailure, block = {
                    moveCaret(input = MoveCaret.Input(position = command.position)).fold(
                        ifLeft = EditorCommand::HandleFailure, ifRight = {
                            EditorCommand.MoveCaretSuccess
                        })
                })
        )

        is EditorCommand.MoveCaretSuccess -> transition(state)

        is EditorCommand.TextSelection.Start -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> transition(state).effect(
                action(key = command.key, fallback = EditorCommand::HandleFailure) {
                    startSelection(input = StartSelection.Input(position = command.position)).fold(
                        ifLeft = EditorCommand::HandleFailure, ifRight = { EditorCommand.TextSelection.StartSuccess })
                })
        }

        is EditorCommand.TextSelection.StartSuccess -> transition(state)

        is EditorCommand.TextSelection.Extend -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> transition(state).effect(
                action(key = command.key, fallback = EditorCommand::HandleFailure) {
                    extendSelection(input = ExtendSelection.Input(position = command.position)).fold(
                        ifLeft = EditorCommand::HandleFailure, ifRight = { EditorCommand.TextSelection.ExtendSuccess })
                })
        }

        is EditorCommand.TextSelection.ExtendSuccess -> transition(state)

        is EditorCommand.Scroll -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> {
                val x = command.x.coerceIn(0f, (command.contentWidth - command.viewportWidth).coerceAtLeast(0f))

                val y = command.y.coerceIn(0f, (command.contentHeight - command.viewportHeight).coerceAtLeast(0f))

                transition(state.copy(scrollbar = state.scrollbar.copy(x = x, y = y)))
            }
        }
    }
}