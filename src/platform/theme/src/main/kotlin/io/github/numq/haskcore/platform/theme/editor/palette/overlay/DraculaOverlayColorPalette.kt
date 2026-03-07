package io.github.numq.haskcore.platform.theme.editor.palette.overlay

internal object DraculaOverlayColorPalette : OverlayColorPalette {
    override val tooltipBackgroundColor = 0xFF3a3d4c.toInt()

    override val tooltipTextColor = 0xFFf8f8f2.toInt()

    override val tooltipBorderColor = 0xFF6b7090.toInt()

    override val errorUnderlineColor = 0xFFff5555.toInt()

    override val warningUnderlineColor = 0xFFffb86c.toInt()

    override val infoUnderlineColor = 0xFF8be9fd.toInt()

    override val hintUnderlineColor = 0xFF50fa7b.toInt()

    override val autocompleteBackgroundColor = 0xFF3a3d4c.toInt()

    override val autocompleteSelectedBackgroundColor = 0xFF242632.toInt()

    override val autocompleteTextColor = 0xFFf8f8f2.toInt()

    override val autocompleteSelectedTextColor = 0xFFf8f8f2.toInt()

    override val autocompleteBorderColor = 0xFF6b7090.toInt()

    override val contextMenuBackgroundColor = 0xFF3a3d4c.toInt()

    override val contextMenuSelectedBackgroundColor = 0xFF242632.toInt()

    override val contextMenuTextColor = 0xFFf8f8f2.toInt()

    override val contextMenuSeparatorColor = 0xFF6b7090.toInt()

    override val dragPreviewColor = 0xFF242632.toInt().withAlpha(alpha = .7f)

    override val dragPreviewBorderColor = 0xFF6b7090.toInt()
}