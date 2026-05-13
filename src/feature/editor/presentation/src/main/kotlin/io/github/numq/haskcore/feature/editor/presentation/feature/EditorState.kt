package io.github.numq.haskcore.feature.editor.presentation.feature

import io.github.numq.haskcore.feature.editor.core.Editor
import io.github.numq.haskcore.feature.editor.presentation.menu.EditorMenu
import io.github.numq.haskcore.feature.editor.presentation.scrollbar.Scrollbar

internal sealed interface EditorState {
    data object Loading : EditorState

    data class Ready(
        val editor: Editor,
        val gutterWidth: Float = 0f,
        val scrollbar: Scrollbar = Scrollbar.ZERO,
        val menu: EditorMenu = EditorMenu.Hidden,
    ) : EditorState
}