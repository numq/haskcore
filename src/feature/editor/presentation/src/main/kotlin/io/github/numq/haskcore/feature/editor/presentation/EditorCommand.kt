package io.github.numq.haskcore.feature.editor.presentation

import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextSnapshot
import io.github.numq.haskcore.feature.editor.core.caret.Caret
import io.github.numq.haskcore.feature.editor.core.highlighting.Highlighting
import io.github.numq.haskcore.feature.editor.core.occurrences.Occurrences
import io.github.numq.haskcore.feature.editor.core.selection.Selection
import kotlinx.coroutines.flow.Flow

internal sealed interface EditorCommand {
    enum class Key {
        INITIALIZE_CARET, INITIALIZE_CARET_SUCCESS, INITIALIZE_HIGHLIGHTING, INITIALIZE_HIGHLIGHTING_SUCCESS, INITIALIZE_OCCURRENCES, INITIALIZE_OCCURRENCES_SUCCESS, INITIALIZE_SELECTION, INITIALIZE_SELECTION_SUCCESS, INITIALIZE_TEXT_SNAPSHOT, INITIALIZE_TEXT_SNAPSHOT_SUCCESS, REQUEST_HIGHLIGHTING_UPDATE, PROCESS_KEY, MOVE_CARET, START_SELECTION, EXTEND_SELECTION
    }

    data class HandleFailure(val throwable: Throwable) : EditorCommand

    data object InitializeCaret : EditorCommand {
        val key = Key.INITIALIZE_CARET
    }

    data class InitializeCaretSuccess(val flow: Flow<Caret>) : EditorCommand {
        val key = Key.INITIALIZE_CARET_SUCCESS
    }

    data class UpdateCaret(val caret: Caret) : EditorCommand

    data object InitializeHighlighting : EditorCommand {
        val key = Key.INITIALIZE_HIGHLIGHTING
    }

    data class InitializeHighlightingSuccess(val flow: Flow<Highlighting>) : EditorCommand {
        val key = Key.INITIALIZE_HIGHLIGHTING_SUCCESS
    }

    data class UpdateHighlighting(val highlighting: Highlighting) : EditorCommand

    data object InitializeOccurrences : EditorCommand {
        val key = Key.INITIALIZE_OCCURRENCES
    }

    data class InitializeOccurrencesSuccess(val flow: Flow<Occurrences>) : EditorCommand {
        val key = Key.INITIALIZE_OCCURRENCES_SUCCESS
    }

    data class UpdateOccurrences(val occurrences: Occurrences) : EditorCommand

    data object InitializeSelection : EditorCommand {
        val key = Key.INITIALIZE_SELECTION
    }

    data class InitializeSelectionSuccess(val flow: Flow<Selection>) : EditorCommand {
        val key = Key.INITIALIZE_SELECTION_SUCCESS
    }

    data class UpdateSelection(val selection: Selection) : EditorCommand

    data object InitializeTextSnapshot : EditorCommand {
        val key = Key.INITIALIZE_TEXT_SNAPSHOT
    }

    data class InitializeTextSnapshotSuccess(val flow: Flow<TextSnapshot>) : EditorCommand {
        val key = Key.INITIALIZE_TEXT_SNAPSHOT_SUCCESS
    }

    data class UpdateTextSnapshot(val snapshot: TextSnapshot) : EditorCommand

    data class RequestHighlightingUpdate(val startLine: Int, val endLine: Int) : EditorCommand {
        val key = Key.REQUEST_HIGHLIGHTING_UPDATE
    }

    data object RequestHighlightingUpdateSuccess : EditorCommand

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
}