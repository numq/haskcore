package io.github.numq.haskcore.platform.theme.editor.palette.codearea

internal object DraculaCodeAreaColorPalette : CodeAreaColorPalette {
    override val textColor = 0xFFf8f8f2.toInt()

    override val selectionColor = 0xFF44475a.toInt().withAlpha(.8f)

    override val selectionInactiveColor = 0xFF3f4152.toInt()

    override val caretColor = 0xFFcccccc.toInt()

    override val caretInactiveColor = 0xFF6272a4.toInt()

    override val guidelineColor = 0xFFf8f8f2.toInt().withAlpha(alpha = .2f)

    override val indentGuideColor = 0xFF3e404b.toInt().withAlpha(alpha = .3f)

    override val bracketMatchColor = 0xFFffff00.toInt()

    override val bracketMatchBackgroundColor = 0xFF747a9d.toInt().withAlpha(alpha = .1f)

    override val searchMatchColor = 0xFF3b244e.toInt().withAlpha(alpha = .3f)

    override val searchMatchBorderColor = 0xFF3b244e.toInt()

    override val usageHighlightBackground = 0xFF353a4d.toInt().withAlpha(.7f)

    override val currentUsageHighlightBackground = 0xFF50fa7b.toInt().withAlpha(alpha = .2f)

    override val linkColor = 0xFF8be9fd.toInt()

    override val linkHoverColor = 0xFFd6acff.toInt()

    override val whitespaceColor = 0xFF6b7090.toInt().withAlpha(alpha = .5f)
}