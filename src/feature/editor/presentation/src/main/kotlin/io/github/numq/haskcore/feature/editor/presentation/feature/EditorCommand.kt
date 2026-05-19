package io.github.numq.haskcore.feature.editor.presentation.feature

import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.feature.editor.core.Editor
import io.github.numq.haskcore.feature.editor.core.analysis.Analysis
import io.github.numq.haskcore.feature.editor.core.syntax.Syntax
import kotlinx.coroutines.flow.Flow

internal sealed interface EditorCommand {
    enum class Key {
        INITIALIZE_EDITOR, INITIALIZE_EDITOR_SUCCESS, UPDATE_EDITOR, INITIALIZE_ANALYSIS_SUCCESS, INITIALIZE_SYNTAX_SUCCESS, UPDATE_VIEWPORT, PROCESS_KEY, MOVE_CARET, START_SELECTION, EXTEND_SELECTION, SCROLL
    }

    data class HandleFailure(val throwable: Throwable) : EditorCommand

    data object InitializeEditor : EditorCommand {
        val key = Key.INITIALIZE_EDITOR
    }

    data class InitializeEditorSuccess(val flow: Flow<Editor>) : EditorCommand {
        val key = Key.INITIALIZE_EDITOR_SUCCESS
    }

    data class UpdateEditor(val editor: Editor) : EditorCommand {
        val key = Key.UPDATE_EDITOR
    }

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
    ) : EditorCommand {
        val key = Key.SCROLL
    }

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