package io.github.numq.haskcore.feature.editor.presentation

import io.github.numq.haskcore.core.feature.*
import io.github.numq.haskcore.feature.editor.core.usecase.*
import kotlinx.coroutines.flow.map

internal class EditorReducer(
    private val observeCaret: ObserveCaret,
    private val observeHighlighting: ObserveHighlighting,
    private val observeOccurrences: ObserveOccurrences,
    private val observeSelection: ObserveSelection,
    private val observeTextSnapshot: ObserveTextSnapshot,
    private val requestHighlightingUpdate: RequestHighlightingUpdate,
    private val processKey: ProcessKey,
    private val moveCaret: MoveCaret,
    private val startSelection: StartSelection,
    private val extendSelection: ExtendSelection,
) : Reducer<EditorState, EditorCommand, EditorEvent> {
    override fun reduce(state: EditorState, command: EditorCommand) = when (command) {
        is EditorCommand.HandleFailure -> transition(state).event(EditorEvent.HandleFailure(throwable = command.throwable))

        is EditorCommand.InitializeCaret -> transition(state).effect(
            action(
                key = command.key, fallback = EditorCommand::HandleFailure, block = {
                    observeCaret(input = Unit).fold(
                        ifLeft = EditorCommand::HandleFailure, ifRight = EditorCommand::InitializeCaretSuccess
                    )
                })
        )

        is EditorCommand.InitializeCaretSuccess -> transition(state).effect(
            stream(
                key = command.key,
                flow = command.flow.map(EditorCommand::UpdateCaret),
                fallback = EditorCommand::HandleFailure
            )
        )

        is EditorCommand.UpdateCaret -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> transition(state.copy(caret = command.caret))
        }

        is EditorCommand.InitializeHighlighting -> transition(state).effect(
            action(
                key = command.key, fallback = EditorCommand::HandleFailure, block = {
                    observeHighlighting(input = Unit).fold(
                        ifLeft = EditorCommand::HandleFailure, ifRight = EditorCommand::InitializeHighlightingSuccess
                    )
                })
        )

        is EditorCommand.InitializeHighlightingSuccess -> transition(state).effect(
            stream(
                key = command.key,
                flow = command.flow.map(EditorCommand::UpdateHighlighting),
                fallback = EditorCommand::HandleFailure
            )
        )

        is EditorCommand.UpdateHighlighting -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> transition(state.copy(highlighting = command.highlighting))
        }

        is EditorCommand.InitializeOccurrences -> transition(state).effect(
            action(
                key = command.key, fallback = EditorCommand::HandleFailure, block = {
                    observeOccurrences(input = Unit).fold(
                        ifLeft = EditorCommand::HandleFailure, ifRight = EditorCommand::InitializeOccurrencesSuccess
                    )
                })
        )

        is EditorCommand.InitializeOccurrencesSuccess -> transition(state).effect(
            stream(
                key = command.key,
                flow = command.flow.map(EditorCommand::UpdateOccurrences),
                fallback = EditorCommand::HandleFailure
            )
        )

        is EditorCommand.UpdateOccurrences -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> transition(state.copy(occurrences = command.occurrences))
        }

        is EditorCommand.InitializeSelection -> transition(state).effect(
            action(
                key = command.key, fallback = EditorCommand::HandleFailure, block = {
                    observeSelection(input = Unit).fold(
                        ifLeft = EditorCommand::HandleFailure, ifRight = EditorCommand::InitializeSelectionSuccess
                    )
                })
        )

        is EditorCommand.InitializeSelectionSuccess -> transition(state).effect(
            stream(
                key = command.key,
                flow = command.flow.map(EditorCommand::UpdateSelection),
                fallback = EditorCommand::HandleFailure
            )
        )

        is EditorCommand.UpdateSelection -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> transition(state.copy(selection = command.selection))
        }

        is EditorCommand.InitializeTextSnapshot -> transition(state).effect(
            action(
                key = command.key, fallback = EditorCommand::HandleFailure, block = {
                    observeTextSnapshot(input = Unit).fold(
                        ifLeft = EditorCommand::HandleFailure, ifRight = EditorCommand::InitializeTextSnapshotSuccess
                    )
                })
        )

        is EditorCommand.InitializeTextSnapshotSuccess -> transition(state).effect(
            stream(
                key = command.key,
                flow = command.flow.map(EditorCommand::UpdateTextSnapshot),
                fallback = EditorCommand::HandleFailure
            )
        )

        is EditorCommand.UpdateTextSnapshot -> when (state) {
            is EditorState.Loading -> transition(EditorState.Ready(snapshot = command.snapshot))

            is EditorState.Ready -> transition(state.copy(snapshot = command.snapshot))
        }

        is EditorCommand.RequestHighlightingUpdate -> transition(state).effect(
            action(
                key = command.key, fallback = EditorCommand::HandleFailure, block = {
                    requestHighlightingUpdate.invoke(
                        input = RequestHighlightingUpdate.Input(
                            startLine = command.startLine, endLine = command.endLine
                        )
                    ).fold(ifLeft = EditorCommand::HandleFailure, ifRight = {
                        EditorCommand.RequestHighlightingUpdateSuccess
                    })
                })
        )

        is EditorCommand.RequestHighlightingUpdateSuccess -> transition(state)

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
    }
}