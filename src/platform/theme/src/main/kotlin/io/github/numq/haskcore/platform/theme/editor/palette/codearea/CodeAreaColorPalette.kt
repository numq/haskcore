package io.github.numq.haskcore.platform.theme.editor.palette.codearea

import io.github.numq.haskcore.platform.theme.editor.palette.ColorPalette

interface CodeAreaColorPalette : ColorPalette {
    val textColor: Int

    val selectionColor: Int

    val selectionInactiveColor: Int

    val caretColor: Int

    val caretInactiveColor: Int

    val guidelineColor: Int

    val indentGuideColor: Int

    val bracketMatchColor: Int

    val bracketMatchBackgroundColor: Int

    val searchMatchColor: Int

    val searchMatchBorderColor: Int

    val usageHighlightBackground: Int

    val currentUsageHighlightBackground: Int

    val linkColor: Int

    val linkHoverColor: Int

    val whitespaceColor: Int
}