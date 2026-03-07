package io.github.numq.haskcore.feature.editor.presentation.layout

import io.github.numq.haskcore.feature.editor.core.highlighting.Highlighting
import io.github.numq.haskcore.feature.editor.presentation.background.BackgroundLayout
import io.github.numq.haskcore.feature.editor.presentation.codearea.CodeAreaLayout
import io.github.numq.haskcore.feature.editor.presentation.gutter.GutterLayout
import io.github.numq.haskcore.feature.editor.presentation.viewport.Viewport
import io.github.numq.haskcore.platform.font.EditorFont
import io.github.numq.haskcore.platform.theme.editor.EditorTheme

internal interface LayoutFactory {
    fun createBackgroundLayout(viewport: Viewport, theme: EditorTheme): BackgroundLayout

    fun createGutterLayout(viewport: Viewport, width: Float, font: EditorFont, theme: EditorTheme): GutterLayout

    fun createCodeAreaLayout(
        viewport: Viewport,
        highlighting: Highlighting,
        gutterWidth: Float,
        scrollX: Float,
        font: EditorFont,
        theme: EditorTheme
    ): CodeAreaLayout
}