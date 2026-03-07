package io.github.numq.haskcore.platform.theme.editor.palette.scrollbar

internal object AlucardScrollbarColorColorPalette : ScrollbarColorColorPalette {
    override val backgroundColor = 0xFFdedccf.toInt().withAlpha(alpha = .5f)

    override val hoverColor = 0xFFdedccf.toInt().withAlpha(alpha = .5f)

    override val activeColor = 0xFFdedccf.toInt().withAlpha(alpha = .5f)

    override val trackColor = 0xFFefeddc.toInt().withAlpha(alpha = .5f)

    override val cornerColor = 0xFFbcbab3.toInt().withAlpha(alpha = .5f)
}