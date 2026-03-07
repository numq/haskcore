package io.github.numq.haskcore.platform.theme.editor.palette.gutter

import io.github.numq.haskcore.platform.theme.editor.palette.ColorPalette

interface GutterColorPalette : ColorPalette {
    val textColor: Int

    val contentColor: Int

    val separatorColor: Int

    val currentLineNumberColor: Int

    val modifiedLineNumberColor: Int

    val breakpointColor: Int

    val breakpointDisabledColor: Int

    val breakpointHitColor: Int

    val foldingCollapsedColor: Int

    val foldingExpandedColor: Int

    val bookmarkColor: Int

    val errorMarkerColor: Int

    val warningMarkerColor: Int

    val infoMarkerColor: Int

    val gitAddedColor: Int

    val gitModifiedColor: Int

    val gitDeletedColor: Int
}