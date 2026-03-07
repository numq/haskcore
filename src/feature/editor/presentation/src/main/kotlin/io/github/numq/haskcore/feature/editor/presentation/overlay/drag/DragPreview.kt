package io.github.numq.haskcore.feature.editor.presentation.overlay.drag

import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect

internal data class DragPreview(val text: String, val position: Point, val bounds: Rect, val isCopy: Boolean = false)