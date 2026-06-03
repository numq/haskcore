package io.github.numq.haskcore.feature.editor.presentation.suggestions

import androidx.compose.ui.geometry.Offset
import io.github.numq.haskcore.feature.editor.core.analysis.CodeSuggestion

internal sealed interface SuggestionState {
    data object Hidden : SuggestionState

    data class Visible(
        val suggestions: List<CodeSuggestion>, val selectedIndex: Int = 0, val offset: Offset,
    ) : SuggestionState
}