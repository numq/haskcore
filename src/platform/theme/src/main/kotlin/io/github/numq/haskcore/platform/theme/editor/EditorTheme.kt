package io.github.numq.haskcore.platform.theme.editor

import io.github.numq.haskcore.platform.theme.editor.palette.background.BackgroundColorPalette
import io.github.numq.haskcore.platform.theme.editor.palette.codearea.CodeAreaColorPalette
import io.github.numq.haskcore.platform.theme.editor.palette.gutter.GutterColorPalette
import io.github.numq.haskcore.platform.theme.editor.palette.highlighting.HighlightingColorPalette
import io.github.numq.haskcore.platform.theme.editor.palette.overlay.OverlayColorPalette
import io.github.numq.haskcore.platform.theme.editor.palette.scrollbar.ScrollbarColorColorPalette

interface EditorTheme {
    val backgroundColorPalette: BackgroundColorPalette

    val gutterColorPalette: GutterColorPalette

    val codeAreaColorPalette: CodeAreaColorPalette

    val overlayColorPalette: OverlayColorPalette

    val scrollbarColorPalette: ScrollbarColorColorPalette

    val highlightingColorPalette: HighlightingColorPalette
}