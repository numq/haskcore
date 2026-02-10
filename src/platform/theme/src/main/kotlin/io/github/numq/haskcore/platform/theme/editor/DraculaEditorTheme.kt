package io.github.numq.haskcore.platform.theme.editor

import io.github.numq.haskcore.platform.theme.editor.palette.background.DraculaBackgroundColorPalette
import io.github.numq.haskcore.platform.theme.editor.palette.codearea.DraculaCodeAreaColorPalette
import io.github.numq.haskcore.platform.theme.editor.palette.gutter.DraculaGutterColorPalette
import io.github.numq.haskcore.platform.theme.editor.palette.highlighting.DraculaHighlightingColorPalette
import io.github.numq.haskcore.platform.theme.editor.palette.overlay.DraculaOverlayColorPalette
import io.github.numq.haskcore.platform.theme.editor.palette.scrollbar.DraculaScrollbarColorColorPalette

internal object DraculaEditorTheme : EditorTheme {
    override val backgroundColorPalette = DraculaBackgroundColorPalette

    override val gutterColorPalette = DraculaGutterColorPalette

    override val codeAreaColorPalette = DraculaCodeAreaColorPalette

    override val overlayColorPalette = DraculaOverlayColorPalette

    override val scrollbarColorPalette = DraculaScrollbarColorColorPalette

    override val highlightingColorPalette = DraculaHighlightingColorPalette
}