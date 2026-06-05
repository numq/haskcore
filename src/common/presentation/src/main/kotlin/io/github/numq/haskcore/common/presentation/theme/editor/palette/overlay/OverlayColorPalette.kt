package io.github.numq.haskcore.common.presentation.theme.editor.palette.overlay

import io.github.numq.haskcore.common.presentation.theme.editor.palette.ColorPalette

interface OverlayColorPalette : ColorPalette {
    val documentationBackgroundColor: Int

    val documentationTextColor: Int

    val documentationBorderColor: Int

    val unknownUnderlineColor: Int

    val errorUnderlineColor: Int

    val warningUnderlineColor: Int

    val infoUnderlineColor: Int

    val hintUnderlineColor: Int

    val suggestionsBackgroundColor: Int

    val suggestionsSelectedBackgroundColor: Int

    val suggestionsTextColor: Int

    val suggestionsSelectedTextColor: Int

    val suggestionsBorderColor: Int

    val contextMenuBackgroundColor: Int

    val contextMenuSelectedBackgroundColor: Int

    val contextMenuTextColor: Int

    val contextMenuSeparatorColor: Int

    val dragPreviewColor: Int

    val dragPreviewBorderColor: Int
}