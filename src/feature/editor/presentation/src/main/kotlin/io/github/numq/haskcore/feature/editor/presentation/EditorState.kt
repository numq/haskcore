package io.github.numq.haskcore.feature.editor.presentation

import io.github.numq.haskcore.core.text.TextSnapshot
import io.github.numq.haskcore.feature.editor.core.caret.Caret
import io.github.numq.haskcore.feature.editor.core.highlighting.Highlighting
import io.github.numq.haskcore.feature.editor.core.occurrences.Occurrences
import io.github.numq.haskcore.feature.editor.core.selection.Selection

internal sealed interface EditorState {
    data object Loading : EditorState

    data class Ready(
        val snapshot: TextSnapshot,
        val caret: Caret = Caret.ZERO,
        val selection: Selection = Selection.EMPTY,
        val highlighting: Highlighting = Highlighting(),
        val occurrences: Occurrences = Occurrences(),
        val gutterWidth: Float = 0f,
        val scrollX: Float = 0f,
        val revision: Long = 0L,
    ) : EditorState
}