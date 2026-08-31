package io.github.numq.haskcore.feature.editor.presentation.feature

import io.github.numq.haskcore.common.presentation.feature.*
import io.github.numq.haskcore.feature.editor.core.token.Token
import io.github.numq.haskcore.feature.editor.core.usecase.*
import io.github.numq.haskcore.feature.editor.presentation.documentation.DocumentationState
import io.github.numq.haskcore.feature.editor.presentation.menu.MenuReducer
import io.github.numq.haskcore.feature.editor.presentation.scrollbar.Scrollbar
import io.github.numq.haskcore.feature.editor.presentation.suggestions.SuggestionsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.milliseconds

internal class EditorReducer(
    private val menuReducer: MenuReducer,
    private val applyCodeSuggestion: ApplyCodeSuggestion,
    private val copySelection: CopySelection,
    private val cutSelection: CutSelection,
    private val extendSelection: ExtendSelection,
    private val getCodeDocumentation: GetCodeDocumentation,
    private val getCodeSuggestions: GetCodeSuggestions,
    private val moveCaret: MoveCaret,
    private val observeAnalysis: ObserveAnalysis,
    private val observeEditor: ObserveEditor,
    private val observeSyntax: ObserveSyntax,
    private val pasteFromClipboard: PasteFromClipboard,
    private val processKey: ProcessKey,
    private val saveEditorPosition: SaveEditorPosition,
    private val selectAll: SelectAll,
    private val startSelection: StartSelection,
    private val updateActiveLines: UpdateActiveLines,
    private val updateCollapsedLines: UpdateCollapsedLines,
    private val updateFoldingRegions: UpdateFoldingRegions,
) : Reducer<EditorState, EditorCommand, EditorEvent> {
    override fun reduce(
        state: EditorState, command: EditorCommand,
    ): Transition<EditorState, EditorEvent> = when (command) {
        is EditorCommand.HandleFailure -> transition(state).event(EditorEvent.HandleFailure(throwable = command.throwable))

        is EditorCommand.Menu -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> menuReducer.reduce(state = state, command = command)
        }

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
            is EditorState.Loading -> transition(
                EditorState.Ready(
                    editor = command.editor, scrollbar = Scrollbar(
                        x = command.editor.position.horizontalOffset, y = command.editor.position.verticalOffset
                    )
                )
            )

            is EditorState.Ready -> transition(state.copy(editor = command.editor))
        }

        is EditorCommand.InitializeAnalysisSuccess -> transition(state).effect(
            stream(
                key = command.key,
                flow = command.flow.map(EditorCommand::UpdateAnalysis),
                fallback = EditorCommand::HandleFailure
            )
        )

        is EditorCommand.UpdateAnalysis -> when (state) {
            is EditorState.Ready -> {
                val currentRevision = state.analysis?.revision?.value ?: -1L

                val incomingRevision = command.analysis?.revision?.value ?: -1L

                when {
                    incomingRevision >= currentRevision -> transition(state.copy(analysis = command.analysis))

                    else -> transition(state)
                }
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
            is EditorState.Ready -> {
                val currentRevision = state.syntax?.revision?.value ?: -1L

                val incomingRevision = command.syntax?.revision?.value ?: -1L

                when {
                    incomingRevision >= currentRevision -> transition(state.copy(syntax = command.syntax)).effect(
                        action(key = "update_folding_regions", fallback = EditorCommand::HandleFailure) {
                            updateFoldingRegions(input = UpdateFoldingRegions.Input(ranges = command.syntax?.foldingRegions?.map { it.range.start.line..it.range.end.line }
                                ?: emptyList())).fold(
                                ifLeft = EditorCommand::HandleFailure,
                                ifRight = { EditorCommand.UpdateViewportSuccess })
                        })

                    else -> transition(state)
                }
            }

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

        is EditorCommand.DocumentationHover.Enter -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> {
                val isAlreadyVisible = (state.documentationState as? DocumentationState.Visible)?.let { visible ->
                    visible.position == command.position || state.isMouseOverDocumentation
                } ?: false

                when {
                    isAlreadyVisible -> transition(state).effect(cancel(key = command.key))

                    else -> transition(state).effect(
                        action(
                            key = command.key, fallback = EditorCommand::HandleFailure, block = {
                                val lineTokens = state.syntax?.tokensPerLine?.get(command.position.line) ?: emptyList()

                                val tokenUnderCursor = lineTokens.find { token ->
                                    command.position.column >= token.range.start.column && command.position.column < token.range.end.column
                                }

                                val isMeaningfulToken = tokenUnderCursor != null && tokenUnderCursor.type !in listOf(
                                    Token.Type.UNKNOWN,
                                    Token.Type.PUNCTUATION_BRACKET,
                                    Token.Type.PUNCTUATION_DELIMITER,
                                    Token.Type.COMMENT,
                                    Token.Type.COMMENT_DOCUMENTATION
                                )

                                when {
                                    !isMeaningfulToken -> {
                                        delay(300.milliseconds)

                                        EditorCommand.DismissDocumentation
                                    }

                                    else -> {
                                        delay(500.milliseconds)

                                        getCodeDocumentation(
                                            input = GetCodeDocumentation.Input(
                                                language = state.editor.language, position = command.position
                                            )
                                        ).fold(
                                            ifLeft = EditorCommand::HandleFailure, ifRight = { documentation ->
                                                when (documentation) {
                                                    null -> EditorCommand.DismissDocumentation

                                                    else -> EditorCommand.ShowDocumentationSuccess(
                                                        position = command.position,
                                                        offset = command.offset,
                                                        documentation = documentation,
                                                    )
                                                }
                                            })
                                    }
                                }
                            })
                    )
                }
            }
        }

        is EditorCommand.DocumentationHover.Exit -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> when {
                state.documentationState is DocumentationState.Visible || state.isMouseOverDocumentation -> transition(
                    state
                ).effect(cancel(key = command.key))

                else -> transition(state).effect(
                    action(
                        key = command.key, fallback = EditorCommand::HandleFailure, block = {
                            delay(300.milliseconds)

                            EditorCommand.DismissDocumentation
                        })
                )
            }
        }

        is EditorCommand.DocumentationHover.SetMouseOver -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> transition(state.copy(isMouseOverDocumentation = command.isOver)).effect(
                when {
                    command.isOver -> cancel(command.key)

                    else -> action(
                        key = command.key, fallback = EditorCommand::HandleFailure, block = {
                            delay(300.milliseconds)

                            EditorCommand.DismissDocumentation
                        })
                }
            )
        }

        is EditorCommand.ShowDocumentationSuccess -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> transition(
                state.copy(
                    documentationState = DocumentationState.Visible(
                        documentation = command.documentation, offset = command.offset, position = command.position
                    )
                )
            )
        }

        is EditorCommand.DismissDocumentation -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> when {
                state.isMouseOverDocumentation -> transition(state)

                else -> when (state.documentationState) {
                    is DocumentationState.Hidden -> transition(state)

                    is DocumentationState.Visible -> transition(state.copy(documentationState = DocumentationState.Hidden))
                }
            }
        }

        is EditorCommand.ShowSuggestions -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> {
                val anchorOffset = (state.suggestionsState as? SuggestionsState.Visible)?.offset ?: command.offset

                transition(state).effect(
                    action(
                        key = command.key, fallback = EditorCommand::HandleFailure, block = {
                            val lineTokens =
                                state.syntax?.tokensPerLine?.get(state.editor.caret.position.line) ?: emptyList()

                            val tokenUnderCursor = lineTokens.find { token ->
                                state.editor.caret.position.column >= token.range.start.column && state.editor.caret.position.column <= token.range.end.column
                            }

                            val isCommentOrString = tokenUnderCursor != null && tokenUnderCursor.type in listOf(
                                Token.Type.COMMENT,
                                Token.Type.COMMENT_DOCUMENTATION,
                                Token.Type.STRING,
                                Token.Type.CHARACTER
                            )

                            if (isCommentOrString) {
                                return@action EditorCommand.DismissSuggestions
                            }

                            delay(100.milliseconds)

                            getCodeSuggestions(
                                input = GetCodeSuggestions.Input(
                                    language = state.editor.language, position = state.editor.caret.position
                                )
                            ).fold(
                                ifLeft = EditorCommand::HandleFailure, ifRight = { suggestions ->
                                    when {
                                        suggestions.isEmpty() -> EditorCommand.DismissSuggestions

                                        else -> EditorCommand.ShowSuggestionsSuccess(
                                            suggestions = suggestions, offset = anchorOffset
                                        )
                                    }
                                })
                        })
                )
            }
        }

        is EditorCommand.ShowSuggestionsSuccess -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> transition(
                state.copy(
                    suggestionsState = SuggestionsState.Visible(
                        suggestions = command.suggestions, offset = command.offset
                    )
                )
            )
        }

        is EditorCommand.UpdateSuggestionsSelection -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> when (val suggestionState = state.suggestionsState) {
                is SuggestionsState.Hidden -> transition(state)

                is SuggestionsState.Visible -> transition(
                    state.copy(
                        suggestionsState = suggestionState.copy(
                            selectedIndex = command.index.coerceIn(
                                0, suggestionState.suggestions.size - 1
                            )
                        )
                    )
                )
            }
        }

        is EditorCommand.ApplySuggestion -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> transition(state).effect(
                action(
                    key = command.key, fallback = EditorCommand::HandleFailure, block = {
                        applyCodeSuggestion(
                            input = ApplyCodeSuggestion.Input(
                                snapshot = state.editor.snapshot,
                                position = state.editor.caret.position,
                                suggestion = command.suggestion
                            )
                        ).fold(
                            ifLeft = EditorCommand::HandleFailure, ifRight = { EditorCommand.DismissSuggestions })
                    })
            )
        }

        is EditorCommand.DismissSuggestions -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> transition(state.copy(suggestionsState = SuggestionsState.Hidden))
        }

        is EditorCommand.ProcessKey -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> when {
                command.keyCode == 111 || command.keyCode == 27 -> when {
                    state.suggestionsState is SuggestionsState.Visible -> transition(
                        state.copy(suggestionsState = SuggestionsState.Hidden)
                    )

                    state.documentationState is DocumentationState.Visible -> transition(
                        state.copy(documentationState = DocumentationState.Hidden)
                    )

                    else -> transition(state)
                }

                else -> transition(state.copy(documentationState = DocumentationState.Hidden))
            }
        }.effect(
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
                            EditorCommand.ProcessKeySuccess(
                                utf16CodePoint = command.utf16CodePoint,
                                offset = command.offset,
                                keyCode = command.keyCode
                            )
                        })
                })
        )

        is EditorCommand.ProcessKeySuccess -> when (state) {
            is EditorState.Ready -> {
                val char = command.utf16CodePoint.toChar()

                val isNavigation = command.keyCode in listOf(
                    19, 20, 21, 22, 37, 38, 39, 40
                )

                when {
                    isNavigation -> transition(state.copy(suggestionsState = SuggestionsState.Hidden))

                    char.isWhitespace() || char == '\u0000' -> transition(
                        state.copy(
                            suggestionsState = SuggestionsState.Hidden
                        )
                    )

                    command.offset != null -> reduce(
                        state, EditorCommand.ShowSuggestions(offset = command.offset)
                    )

                    else -> transition(state)
                }
            }

            else -> transition(state)
        }

        is EditorCommand.MoveCaret -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> transition(
                state.copy(
                    documentationState = DocumentationState.Hidden, suggestionsState = SuggestionsState.Hidden
                )
            )
        }.effect(
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

            is EditorState.Ready -> transition(
                state.copy(
                    documentationState = DocumentationState.Hidden, suggestionsState = SuggestionsState.Hidden
                )
            ).effect(
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
                val x = command.x.coerceIn(
                    0f, (command.contentWidth - command.viewportWidth).coerceAtLeast(0f)
                )

                val y = command.y.coerceIn(
                    0f, (command.contentHeight - command.viewportHeight).coerceAtLeast(0f)
                )

                transition(
                    state.copy(
                        scrollbar = state.scrollbar.copy(x = x, y = y), documentationState = DocumentationState.Hidden
                    )
                )
            }
        }

        is EditorCommand.SaveEditorPosition -> transition(state).effect(
            action(
                key = command.key, fallback = EditorCommand::HandleFailure, block = {
                    saveEditorPosition(
                        input = SaveEditorPosition.Input(position = command.position)
                    ).fold(ifLeft = EditorCommand::HandleFailure, ifRight = {
                        EditorCommand.SaveEditorPositionSuccess
                    })
                })
        )

        is EditorCommand.SaveEditorPositionSuccess -> transition(state)

        is EditorCommand.ToggleFolding -> when (state) {
            is EditorState.Loading -> transition(state)

            is EditorState.Ready -> {
                val newCollapsedLines = when {
                    state.collapsedLines.contains(command.line) -> state.collapsedLines - command.line

                    else -> state.collapsedLines + command.line
                }

                transition(state.copy(collapsedLines = newCollapsedLines)).effect(
                    action(key = command.key, fallback = EditorCommand::HandleFailure) {
                        updateCollapsedLines(input = UpdateCollapsedLines.Input(lines = newCollapsedLines)).fold(
                            ifLeft = EditorCommand::HandleFailure, ifRight = { EditorCommand.UpdateViewportSuccess })
                    })
            }
        }
    }
}