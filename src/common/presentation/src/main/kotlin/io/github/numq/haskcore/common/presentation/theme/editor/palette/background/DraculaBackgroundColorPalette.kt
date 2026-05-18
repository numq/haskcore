package io.github.numq.haskcore.common.presentation.theme.editor.palette.background

internal object DraculaBackgroundColorPalette : BackgroundColorPalette {
    override val backgroundColor = 0xFF282a36.toInt()

    override val backgroundOutlineColor = 0xFF44475a.toInt()

    override val currentLineColor = 0xFF44475a.toInt().withAlpha(.4f)
}