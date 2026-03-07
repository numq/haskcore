package io.github.numq.haskcore.feature.editor.presentation.overlay.completion

import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect

internal data class AutocompleteBox(
    val position: Point, val items: List<AutocompleteItem>, val selectedIndex: Int = 0, val bounds: Rect
)