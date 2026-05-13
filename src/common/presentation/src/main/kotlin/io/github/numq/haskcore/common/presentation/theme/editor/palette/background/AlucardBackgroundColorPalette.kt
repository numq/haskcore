package io.github.numq.haskcore.common.presentation.theme.editor.palette.background

internal object AlucardBackgroundColorPalette : BackgroundColorPalette {
    override val backgroundColor = 0xFFfffbeb.toInt()

    override val currentLineColor = 0xFFdedccf.toInt().withAlpha(.4f)
}