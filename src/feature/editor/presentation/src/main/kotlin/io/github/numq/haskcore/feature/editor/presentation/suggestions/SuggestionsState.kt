package io.github.numq.haskcore.feature.editor.presentation.suggestions

import androidx.compose.ui.geometry.Offset
import io.github.numq.haskcore.feature.editor.core.analysis.CodeSuggestion

internal sealed interface SuggestionsState {
    data object Hidden : SuggestionsState

    data class Visible(
        val suggestions: List<CodeSuggestion>, val offset: Offset, val selectedIndex: Int = 0,
    ) : SuggestionsState
}