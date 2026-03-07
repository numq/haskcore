package io.github.numq.haskcore.feature.editor.presentation.layer

import org.jetbrains.skia.Canvas

internal interface Layer {
    fun render(canvas: Canvas)
}