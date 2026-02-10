package io.github.numq.haskcore.platform.theme.editor

import io.github.numq.haskcore.platform.theme.editor.palette.background.AlucardBackgroundColorPalette
import io.github.numq.haskcore.platform.theme.editor.palette.codearea.AlucardCodeAreaColorPalette
import io.github.numq.haskcore.platform.theme.editor.palette.gutter.AlucardGutterColorPalette
import io.github.numq.haskcore.platform.theme.editor.palette.highlighting.AlucardHighlightingColorPalette
import io.github.numq.haskcore.platform.theme.editor.palette.overlay.AlucardOverlayColorPalette
import io.github.numq.haskcore.platform.theme.editor.palette.scrollbar.AlucardScrollbarColorColorPalette

internal object AlucardEditorTheme : EditorTheme {
    override val backgroundColorPalette = AlucardBackgroundColorPalette

    override val gutterColorPalette = AlucardGutterColorPalette

    override val codeAreaColorPalette = AlucardCodeAreaColorPalette

    override val overlayColorPalette = AlucardOverlayColorPalette

    override val scrollbarColorPalette = AlucardScrollbarColorColorPalette

    override val highlightingColorPalette = AlucardHighlightingColorPalette
}