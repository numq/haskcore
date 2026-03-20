package io.github.numq.haskcore.platform.theme.editor.palette.codearea

internal object AlucardCodeAreaColorPalette : CodeAreaColorPalette {
    override val textColor = 0xFF1f1f1f.toInt()

    override val selectionColor = 0xFFc6c4b8.toInt()

    override val selectionInactiveColor = 0xFFcfcdbe.toInt()

    override val caretColor = 0xFF1f1f1f.toInt()

    override val caretInactiveColor = 0xFF6c664b.toInt()

    override val guidelineColor = 0xFF1f1f1f.toInt().withAlpha(alpha = .2f)

    override val indentGuideColor = 0xFFbcbab3.toInt()

    override val bracketMatchColor = 0xFFa34d14.toInt()

    override val bracketMatchBackgroundColor = 0xFFa34d14.toInt().withAlpha(.2f)

    override val searchMatchColor = 0xFFf0e5b6.toInt().withAlpha(.5f)

    override val searchMatchBorderColor = 0xFFdedccf.toInt()

    override val usageHighlightBackground = 0xFF9c9a90.toInt().withAlpha(.4f)

    override val currentUsageHighlightBackground = 0xFFa3144d.toInt().withAlpha(.4f)

    override val linkColor = 0xFF036a96.toInt()

    override val linkHoverColor = 0xFF047fb4.toInt()

    override val whitespaceColor = 0xFFbcbab3.toInt()
}