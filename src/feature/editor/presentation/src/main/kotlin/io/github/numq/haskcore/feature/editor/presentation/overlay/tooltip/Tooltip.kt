package io.github.numq.haskcore.feature.editor.presentation.overlay.tooltip

import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect

internal data class Tooltip(
    val id: String,
    val text: String,
    val bounds: Rect,
    val anchor: Point? = null,
    val type: TooltipType = TooltipType.INFO
)