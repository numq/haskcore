package io.github.numq.haskcore.common.presentation.theme.editor.palette.background

import io.github.numq.haskcore.common.presentation.theme.editor.palette.ColorPalette

interface BackgroundColorPalette : ColorPalette {
    val backgroundColor: Int

    val backgroundOutlineColor: Int

    val currentLineColor: Int
}