package io.github.numq.haskcore.feature.editor.presentation.documentation

import androidx.compose.ui.geometry.Offset
import io.github.numq.haskcore.feature.editor.core.analysis.CodeDocumentation

internal sealed interface DocumentationState {
    data object Hidden : DocumentationState

    data class Visible(val documentation: CodeDocumentation, val position: Offset) : DocumentationState
}