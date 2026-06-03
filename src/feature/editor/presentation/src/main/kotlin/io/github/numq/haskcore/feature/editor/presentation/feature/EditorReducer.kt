package io.github.numq.haskcore.feature.editor.presentation.feature

import androidx.compose.ui.geometry.Offset
import io.github.numq.haskcore.common.presentation.feature.*
import io.github.numq.haskcore.feature.editor.core.usecase.*
import io.github.numq.haskcore.feature.editor.presentation.documentation.DocumentationState
import io.github.numq.haskcore.feature.editor.presentation.menu.MenuReducer
import io.github.numq.haskcore.feature.editor.presentation.suggestions.SuggestionState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class EditorReducer(
    private val menuReducer: MenuReducer,
    private val observeAnalysis: ObserveAnalysis,
    private val observeEditor: ObserveEditor,
    private val observeSyntax: ObserveSyntax,
    private val updateActiveLines: UpdateActiveLines,
    private val requestCodeDocumentation: RequestCodeDocumentation,
    private val dismissCodeDocumentation: DismissCodeDocumentation,
    private val applyCodeSuggestion: ApplyCodeSuggestion,
    private val processKey: ProcessKey,
    private val moveCaret: MoveCaret,
    private val startSelection: StartSelection,
    private val extendSelection: ExtendSelection,
) : Reducer<EditorState, EditorCommand, EditorEvent> {
    override fun reduce(
        state: EditorState, command: EditorCommand,
    ): Transition<EditorState, EditorEvent> = when (command) {
        is EditorCommand.Menu -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> menuReducer.reduce(state = state, command = command)
        }

        is EditorCommand.HandleFailure -> transition(state).event(EditorEvent.HandleFailure(throwable = command.throwable))

        is EditorCommand.Initialize -> with(command) {
            transition(state).effects(
                action(
                    key = keyEditor, fallback = EditorCommand::HandleFailure, block = {
                        observeEditor(input = ObserveEditor.Input(path = path)).fold(
                            ifLeft = EditorCommand::HandleFailure, ifRight = EditorCommand::InitializeEditorSuccess
                        )
                    }), action(
                    key = keyAnalysis, fallback = EditorCommand::HandleFailure, block = {
                        observeAnalysis(
                            input = ObserveAnalysis.Input(path = path, language = language)
                        ).fold(
                            ifLeft = EditorCommand::HandleFailure, ifRight = EditorCommand::InitializeAnalysisSuccess
                        )
                    }), action(
                    key = keySyntax, fallback = EditorCommand::HandleFailure, block = {
                        observeSyntax(input = ObserveSyntax.Input(language = language)).fold(
                            ifLeft = EditorCommand::HandleFailure, ifRight = EditorCommand::InitializeSyntaxSuccess
                        )
                    })
            )
        }

        is EditorCommand.InitializeEditorSuccess -> transition(state).effect(
            stream(
                key = command.key,
                flow = command.flow.map(EditorCommand::UpdateEditor),
                fallback = EditorCommand::HandleFailure
            )
        )

        is EditorCommand.UpdateEditor -> when (state) {
            is EditorState.Loading -> transition(EditorState.Ready(editor = command.editor))

            is EditorState.Ready -> {
                val isCaretChanged = command.editor.caret.position != state.editor.caret.position

                transition(
                    state.copy(
                        editor = command.editor, suggestionState = when {
                            isCaretChanged -> SuggestionState.Hidden

                            else -> state.suggestionState
                        }, documentationState = when {
                            isCaretChanged -> DocumentationState.Hidden

                            else -> state.documentationState
                        }
                    )
                )
            }
        }

        is EditorCommand.InitializeAnalysisSuccess -> transition(state).effect(
            stream(
                key = command.key,
                flow = command.flow.map(EditorCommand::UpdateAnalysis),
                fallback = EditorCommand::HandleFailure
            )
        )

        is EditorCommand.UpdateAnalysis -> when (state) {
            is EditorState.Ready if command.analysis?.revision == state.editor.snapshot.revision -> {
                val newState = state.copy(analysis = command.analysis)

                val suggestions = command.analysis.suggestions

                val updatedState = when {
                    suggestions.isNotEmpty() -> when (state.suggestionState) {
                        is SuggestionState.Visible -> {
                            newState.copy(
                                suggestionState = state.suggestionState.copy(
                                    suggestions = suggestions,
                                    selectedIndex = state.suggestionState.selectedIndex.coerceIn(
                                        0, suggestions.size - 1
                                    )
                                )
                            )
                        }

                        is SuggestionState.Hidden -> newState.copy(
                            suggestionState = SuggestionState.Visible(
                                suggestions = suggestions, offset = Offset.Zero
                            )
                        )
                    }

                    else -> newState.copy(suggestionState = SuggestionState.Hidden)
                }

                transition(updatedState)
            }

            else -> transition(state)
        }

        is EditorCommand.InitializeSyntaxSuccess -> transition(state).effect(
            stream(
                key = command.key,
                flow = command.flow.distinctUntilChanged().map(EditorCommand::UpdateSyntax),
                fallback = EditorCommand::HandleFailure
            )
        )

        is EditorCommand.UpdateSyntax -> when (state) {
            is EditorState.Ready if command.syntax?.revision == state.editor.snapshot.revision -> transition(
                state.copy(syntax = command.syntax)
            )

            else -> transition(state)
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

        is EditorCommand.ShowDocumentation -> when (state) {
            is EditorState.Ready -> transition(
                state.copy(
                    documentationState = DocumentationState.Visible(
                        documentation = command.documentation, position = command.position
                    )
                )
            )

            else -> transition(state)
        }

        is EditorCommand.RequestDocumentation -> transition(state).effect(
            action(
                key = command.key, fallback = EditorCommand::HandleFailure, block = {
                    requestCodeDocumentation(input = RequestCodeDocumentation.Input(position = command.position)).fold(
                        ifLeft = EditorCommand::HandleFailure, ifRight = {
                            EditorCommand.RequestDocumentationSuccess
                        })
                })
        )

        is EditorCommand.RequestDocumentationSuccess -> transition(state)

        is EditorCommand.DismissDocumentation -> transition(state).effect(
            action(
                key = command.key, fallback = EditorCommand::HandleFailure, block = {
                    dismissCodeDocumentation(input = Unit).fold(ifLeft = EditorCommand::HandleFailure, ifRight = {
                        EditorCommand.ProcessKeySuccess
                    })
                })
        )

        is EditorCommand.DismissDocumentationSuccess -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> transition(state.copy(documentationState = DocumentationState.Hidden))
        }

        is EditorCommand.ShowSuggestions -> when (state) {
            is EditorState.Ready -> transition(
                state.copy(
                    suggestionState = SuggestionState.Visible(
                        suggestions = command.suggestions, offset = command.offset
                    )
                )
            )

            else -> transition(state)
        }

        is EditorCommand.UpdateSuggestionsSelection -> when (state) {
            is EditorState.Ready -> when (val suggestionState = state.suggestionState) {
                is SuggestionState.Visible -> transition(
                    state.copy(
                        suggestionState = suggestionState.copy(
                            selectedIndex = command.index.coerceIn(
                                0, suggestionState.suggestions.size - 1
                            )
                        )
                    )
                )

                else -> transition(state)
            }

            else -> transition(state)
        }

        is EditorCommand.ApplySuggestion -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> transition(state.copy(suggestionState = SuggestionState.Hidden)).effect(
                action(
                    key = command.key, fallback = EditorCommand::HandleFailure, block = {
                        applyCodeSuggestion(
                            input = ApplyCodeSuggestion.Input(
                                snapshot = state.editor.snapshot,
                                position = state.editor.caret.position,
                                suggestion = command.suggestion
                            )
                        ).fold(
                            ifLeft = EditorCommand::HandleFailure, ifRight = {
                                EditorCommand.ApplySuggestionSuccess
                            })
                    })
            )
        }

        is EditorCommand.ApplySuggestionSuccess -> transition(state)

        is EditorCommand.DismissSuggestions -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> transition(state.copy(suggestionState = SuggestionState.Hidden))
        }

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

        is EditorCommand.MoveCaretSuccess -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> transition(
                state.copy(suggestionState = SuggestionState.Hidden, documentationState = DocumentationState.Hidden)
            )
        }

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

                transition(
                    state.copy(
                        scrollbar = state.scrollbar.copy(x = x, y = y), documentationState = DocumentationState.Hidden
                    )
                )
            }
        }
    }
}