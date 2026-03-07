package io.github.numq.haskcore.feature.editor.presentation.layout

import io.github.numq.haskcore.feature.editor.core.highlighting.Highlighting
import io.github.numq.haskcore.feature.editor.presentation.background.BackgroundLayout
import io.github.numq.haskcore.feature.editor.presentation.codearea.CodeAreaLayout
import io.github.numq.haskcore.feature.editor.presentation.gutter.GutterLayout
import io.github.numq.haskcore.feature.editor.presentation.layer.LayerFactory
import io.github.numq.haskcore.feature.editor.presentation.viewport.Viewport
import io.github.numq.haskcore.platform.font.EditorFont
import io.github.numq.haskcore.platform.theme.editor.EditorTheme
import org.jetbrains.skia.Rect

internal class SkiaLayoutFactory(private val layerFactory: LayerFactory) : LayoutFactory {
    override fun createBackgroundLayout(viewport: Viewport, theme: EditorTheme): BackgroundLayout {
        val bounds = Rect.makeWH(w = viewport.width, h = viewport.height)

        val backgroundLayer = layerFactory.createBackgroundLayer(bounds = bounds, theme = theme)

        return BackgroundLayout(viewport = viewport, bounds = bounds, backgroundLayer = backgroundLayer)
    }

    override fun createGutterLayout(
        viewport: Viewport, width: Float, font: EditorFont, theme: EditorTheme
    ): GutterLayout {
        val bounds = Rect.makeWH(w = width, h = viewport.height)

        val separatorLayer = layerFactory.createGutterSeparatorLayer(x = bounds.width, y = bounds.height, theme = theme)

        val gutterLineLayers = viewport.viewportLines.map { viewportLine ->
            layerFactory.createGutterLineLayer(
                line = viewportLine.line,
                width = bounds.width,
                textY = viewportLine.textBaselineY,
                font = font,
                theme = theme
            )
        }

        return GutterLayout(
            viewport = viewport, bounds = bounds, separatorLayer = separatorLayer, lineLayers = gutterLineLayers
        )
    }

    override fun createCodeAreaLayout(
        viewport: Viewport,
        highlighting: Highlighting,
        gutterWidth: Float,
        scrollX: Float,
        font: EditorFont,
        theme: EditorTheme
    ): CodeAreaLayout {
        val bounds = Rect.makeWH(w = viewport.width - gutterWidth, h = viewport.height)

        val contentLayers = layerFactory.createCodeAreaContentLayers(
            viewportLines = viewport.viewportLines,
            highlighting = highlighting,
            scrollX = scrollX,
            font = font,
            theme = theme
        )

        return CodeAreaLayout(viewport = viewport, bounds = bounds, contentLayers = contentLayers)
    }
}