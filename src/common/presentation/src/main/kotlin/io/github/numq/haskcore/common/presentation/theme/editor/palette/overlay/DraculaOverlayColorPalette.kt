package io.github.numq.haskcore.common.presentation.theme.editor.palette.overlay

internal object DraculaOverlayColorPalette : OverlayColorPalette {
    override val documentationBackgroundColor = 0xFF3a3d4c.toInt()

    override val documentationTextColor = 0xFFf8f8f2.toInt()

    override val documentationBorderColor = 0xFF6b7090.toInt()

    override val unknownUnderlineColor = 0xFF6b7090.toInt()

    override val errorUnderlineColor = 0xFFff5555.toInt()

    override val warningUnderlineColor = 0xFFffb86c.toInt()

    override val infoUnderlineColor = 0xFF8be9fd.toInt()

    override val hintUnderlineColor = 0xFF50fa7b.toInt()

    override val suggestionsBackgroundColor = 0xFF3a3d4c.toInt()

    override val suggestionsSelectedBackgroundColor = 0xFF242632.toInt()

    override val suggestionsTextColor = 0xFFf8f8f2.toInt()

    override val suggestionsSelectedTextColor = 0xFFf8f8f2.toInt()

    override val suggestionsBorderColor = 0xFF6b7090.toInt()

    override val contextMenuBackgroundColor = 0xFF3a3d4c.toInt()

    override val contextMenuSelectedBackgroundColor = 0xFF242632.toInt()

    override val contextMenuTextColor = 0xFFf8f8f2.toInt()

    override val contextMenuSeparatorColor = 0xFF6b7090.toInt()

    override val dragPreviewColor = 0xFF242632.toInt().withAlpha(alpha = .7f)

    override val dragPreviewBorderColor = 0xFF6b7090.toInt()
}