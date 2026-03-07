package io.github.numq.haskcore.feature.editor.presentation.overlay.menu

import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect

internal data class ContextMenu(val position: Point, val items: List<MenuItem>, val bounds: Rect)