package io.github.numq.haskcore.feature.editor.presentation.feature

import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.feature.editor.core.Editor
import kotlinx.coroutines.flow.Flow

internal sealed interface EditorCommand {
    enum class Key {
        INITIALIZE, INITIALIZE_SUCCESS, UPDATE_VIEWPORT, PROCESS_KEY, MOVE_CARET, START_SELECTION, EXTEND_SELECTION, SCROLL
    }

    data class HandleFailure(val throwable: Throwable) : EditorCommand

    data object Initialize : EditorCommand {
        val key = Key.INITIALIZE
    }

    data class InitializeSuccess(val flow: Flow<Editor>) : EditorCommand {
        val key = Key.INITIALIZE_SUCCESS
    }

    data class UpdateEditor(val editor: Editor) : EditorCommand

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
        val viewportHeight: Float
    ) : EditorCommand {
        val key = Key.SCROLL
    }
}