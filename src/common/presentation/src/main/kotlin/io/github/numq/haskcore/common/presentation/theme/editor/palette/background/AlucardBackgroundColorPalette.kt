package io.github.numq.haskcore.common.presentation.theme.editor.palette.background

internal object AlucardBackgroundColorPalette : BackgroundColorPalette {
    override val backgroundColor = 0xFFfffbeb.toInt()

    override val backgroundOutlineColor = 0xFFa8a493.toInt()

    override val currentLineColor = 0xFFdedccf.toInt().withAlpha(.2f)
}