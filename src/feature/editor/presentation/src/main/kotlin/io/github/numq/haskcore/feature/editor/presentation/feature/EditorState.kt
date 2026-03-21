package io.github.numq.haskcore.feature.editor.presentation.feature

import io.github.numq.haskcore.feature.editor.core.Editor
import io.github.numq.haskcore.feature.editor.presentation.scrollbar.Scrollbar

internal sealed interface EditorState {
    data object Loading : EditorState

    data class Ready(
        val editor: Editor, val scrollbar: Scrollbar = Scrollbar.ZERO, val gutterWidth: Float = 0f
    ) : EditorState
}