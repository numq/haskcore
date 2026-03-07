package io.github.numq.haskcore.platform.theme.editor.palette.scrollbar

import io.github.numq.haskcore.platform.theme.editor.palette.ColorPalette

interface ScrollbarColorColorPalette : ColorPalette {
    val backgroundColor: Int

    val hoverColor: Int

    val activeColor: Int

    val trackColor: Int

    val cornerColor: Int
}