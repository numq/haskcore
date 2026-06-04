package io.github.numq.haskcore.feature.editor.presentation.feature

import androidx.compose.ui.geometry.Offset
import io.github.numq.haskcore.common.core.language.Language
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.feature.editor.core.Editor
import io.github.numq.haskcore.feature.editor.core.analysis.Analysis
import io.github.numq.haskcore.feature.editor.core.analysis.CodeDocumentation
import io.github.numq.haskcore.feature.editor.core.analysis.CodeSuggestion
import io.github.numq.haskcore.feature.editor.core.syntax.Syntax
import kotlinx.coroutines.flow.Flow

internal sealed interface EditorCommand {
    enum class Key {
        INITIALIZE_EDITOR, INITIALIZE_EDITOR_SUCCESS, INITIALIZE_ANALYSIS, INITIALIZE_ANALYSIS_SUCCESS, INITIALIZE_SYNTAX, INITIALIZE_SYNTAX_SUCCESS, UPDATE_VIEWPORT, REQUEST_DOCUMENTATION, DISMISS_DOCUMENTATION, COMPLETE_CODE, PROCESS_KEY, MOVE_CARET, START_SELECTION, EXTEND_SELECTION
    }

    data class HandleFailure(val throwable: Throwable) : EditorCommand

    data class Initialize(val path: String, val language: Language) : EditorCommand {
        val keyEditor = Key.INITIALIZE_EDITOR

        val keyAnalysis = Key.INITIALIZE_ANALYSIS

        val keySyntax = Key.INITIALIZE_SYNTAX
    }

    data class InitializeEditorSuccess(val flow: Flow<Editor>) : EditorCommand {
        val key = Key.INITIALIZE_EDITOR_SUCCESS
    }

    data class UpdateEditor(val editor: Editor) : EditorCommand

    data class InitializeAnalysisSuccess(val flow: Flow<Analysis?>) : EditorCommand {
        val key = Key.INITIALIZE_ANALYSIS_SUCCESS
    }

    data class UpdateAnalysis(val analysis: Analysis?) : EditorCommand

    data class InitializeSyntaxSuccess(val flow: Flow<Syntax?>) : EditorCommand {
        val key = Key.INITIALIZE_SYNTAX_SUCCESS
    }

    data class UpdateSyntax(val syntax: Syntax?) : EditorCommand

    data class UpdateViewport(val start: Int, val end: Int) : EditorCommand {
        val key = Key.UPDATE_VIEWPORT
    }

    data object UpdateViewportSuccess : EditorCommand

    data class ShowDocumentation(val documentation: CodeDocumentation, val offset: Offset) : EditorCommand

    data class RequestDocumentation(val position: TextPosition, val offset: Offset) : EditorCommand {
        val key = Key.REQUEST_DOCUMENTATION
    }

    data object RequestDocumentationSuccess : EditorCommand

    data object DismissDocumentation : EditorCommand {
        val key = Key.DISMISS_DOCUMENTATION
    }

    data object DismissDocumentationSuccess : EditorCommand

    data class ShowSuggestions(
        val suggestions: List<CodeSuggestion>, val offset: Offset, val selectedIndex: Int = 0,
    ) : EditorCommand

    data class UpdateSuggestionsSelection(val index: Int) : EditorCommand

    data class ApplySuggestion(val suggestion: CodeSuggestion) : EditorCommand {
        val key = Key.COMPLETE_CODE
    }

    data object ApplySuggestionSuccess : EditorCommand

    data object DismissSuggestions : EditorCommand

    data class ProcessKey(val keyCode: Int, val modifiers: Int, val utf16CodePoint: Int) : EditorCommand {
        val key = Key.PROCESS_KEY
    }

    data object ProcessKeySuccess : EditorCommand

    data class MoveCaret(val position: TextPosition) : EditorCommand {
        val key = Key.MOVE_CARET
    }

    data object MoveCaretSuccess : EditorCommand

    sealed interface TextSelection : EditorCommand {
        data class Start(val position: TextPosition) : TextSelection {
            val key = Key.START_SELECTION
        }

        data object StartSuccess : TextSelection

        data class Extend(val position: TextPosition) : TextSelection {
            val key = Key.EXTEND_SELECTION
        }

        data object ExtendSuccess : TextSelection
    }

    data class Scroll(
        val x: Float,
        val y: Float,
        val contentWidth: Float,
        val contentHeight: Float,
        val viewportWidth: Float,
        val viewportHeight: Float,
    ) : EditorCommand

    sealed interface Menu : EditorCommand {
        data class Open(val x: Float, val y: Float) : Menu

        data object Close : Menu

        data object RunStack : Menu

        data object RunCabal : Menu

        data object RunGhc : Menu

        data object Cut : Menu

        data object Copy : Menu

        data object Paste : Menu

        data object SelectAll : Menu
    }
}