package io.github.numq.haskcore.feature.editor.presentation.overlay.highlight

import org.jetbrains.skia.Color
import org.jetbrains.skia.Rect

internal data class Highlight(
    val id: String, val bounds: Rect, val color: Color, val type: HighlightType = HighlightType.SELECTION
)