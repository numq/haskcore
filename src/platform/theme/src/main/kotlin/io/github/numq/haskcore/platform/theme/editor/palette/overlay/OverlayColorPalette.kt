package io.github.numq.haskcore.platform.theme.editor.palette.overlay

import io.github.numq.haskcore.platform.theme.editor.palette.ColorPalette

interface OverlayColorPalette : ColorPalette {
    val tooltipBackgroundColor: Int

    val tooltipTextColor: Int

    val tooltipBorderColor: Int

    val errorUnderlineColor: Int

    val warningUnderlineColor: Int

    val infoUnderlineColor: Int

    val hintUnderlineColor: Int

    val autocompleteBackgroundColor: Int

    val autocompleteSelectedBackgroundColor: Int

    val autocompleteTextColor: Int

    val autocompleteSelectedTextColor: Int

    val autocompleteBorderColor: Int

    val contextMenuBackgroundColor: Int

    val contextMenuSelectedBackgroundColor: Int

    val contextMenuTextColor: Int

    val contextMenuSeparatorColor: Int

    val dragPreviewColor: Int

    val dragPreviewBorderColor: Int
}