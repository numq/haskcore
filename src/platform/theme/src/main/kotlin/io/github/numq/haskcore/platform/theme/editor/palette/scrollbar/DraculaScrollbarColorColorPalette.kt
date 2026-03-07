package io.github.numq.haskcore.platform.theme.editor.palette.scrollbar

internal object DraculaScrollbarColorColorPalette : ScrollbarColorColorPalette {
    override val backgroundColor = 0xFF3a3d4c.toInt().withAlpha(alpha = .5f)

    override val hoverColor = 0xFF3a3d4c.toInt().withAlpha(alpha = .5f)

    override val activeColor = 0xFF3a3d4c.toInt()

    override val trackColor = 0xFF1e1f29.toInt().withAlpha(alpha = .3f)

    override val cornerColor = 0xFF3a3d4c.toInt().withAlpha(alpha = .3f)
}