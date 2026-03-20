package io.github.numq.haskcore.platform.theme.editor.palette.background

internal object AlucardBackgroundColorPalette : BackgroundColorPalette {
    override val backgroundColor = 0xFFfffbeb.toInt()

    override val currentLineColor = 0xFFdedccf.toInt().withAlpha(.4f)
}