package io.github.numq.haskcore.feature.editor.presentation.feature

import io.github.numq.haskcore.feature.editor.core.Editor
import io.github.numq.haskcore.feature.editor.core.analysis.Analysis
import io.github.numq.haskcore.feature.editor.core.syntax.Syntax
import io.github.numq.haskcore.feature.editor.presentation.documentation.DocumentationState
import io.github.numq.haskcore.feature.editor.presentation.menu.EditorMenu
import io.github.numq.haskcore.feature.editor.presentation.scrollbar.Scrollbar
import io.github.numq.haskcore.feature.editor.presentation.suggestions.SuggestionsState

internal sealed interface EditorState {
    data object Loading : EditorState

    data class Ready(
        val editor: Editor,
        val gutterWidth: Float = 0f,
        val scrollbar: Scrollbar = Scrollbar.ZERO,
        val menu: EditorMenu = EditorMenu.Hidden,
        val analysis: Analysis? = null,
        val syntax: Syntax? = null,
        val documentationState: DocumentationState = DocumentationState.Hidden,
        val suggestionsState: SuggestionsState = SuggestionsState.Hidden,
    ) : EditorState
}