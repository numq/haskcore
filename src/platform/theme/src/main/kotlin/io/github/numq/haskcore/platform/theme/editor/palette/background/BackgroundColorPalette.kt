package io.github.numq.haskcore.platform.theme.editor.palette.background

import io.github.numq.haskcore.platform.theme.editor.palette.ColorPalette

interface BackgroundColorPalette : ColorPalette {
    val backgroundColor: Int

    val currentLineColor: Int
}