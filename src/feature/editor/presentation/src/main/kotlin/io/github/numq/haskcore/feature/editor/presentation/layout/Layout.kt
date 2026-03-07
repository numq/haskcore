package io.github.numq.haskcore.feature.editor.presentation.layout

import io.github.numq.haskcore.feature.editor.presentation.viewport.Viewport
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect

internal interface Layout {
    val viewport: Viewport

    val bounds: Rect

    fun render(canvas: Canvas)
}