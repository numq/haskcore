package io.github.numq.haskcore.feature.editor.presentation

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.editor.presentation.cache.PaintCache
import io.github.numq.haskcore.feature.editor.presentation.cache.ParagraphCache
import io.github.numq.haskcore.feature.editor.presentation.cache.TextLineCache
import io.github.numq.haskcore.feature.editor.presentation.layer.LayerFactory
import io.github.numq.haskcore.feature.editor.presentation.layer.SkiaLayerFactory
import io.github.numq.haskcore.feature.editor.presentation.layout.LayoutFactory
import io.github.numq.haskcore.feature.editor.presentation.layout.SkiaLayoutFactory
import org.koin.dsl.bind
import org.koin.dsl.module

val editorPresentationModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner { TextLineCache(capacity = 1000) }

        scopedOwner { PaintCache(capacity = 1000) }

        scopedOwner { ParagraphCache(capacity = 1000) }

        scopedOwner {
            SkiaLayerFactory(textLineCache = get(), paintCache = get(), paragraphCache = get())
        } bind LayerFactory::class

        scopedOwner { SkiaLayoutFactory(layerFactory = get()) } bind LayoutFactory::class
    }

    scope<ScopeQualifier.Document> {
        scopedOwner {
            EditorReducer(
                observeCaret = get(),
                observeHighlighting = get(),
                observeOccurrences = get(),
                observeSelection = get(),
                observeTextSnapshot = get(),
                requestHighlightingUpdate = get(),
                processKey = get(),
                moveCaret = get(),
                startSelection = get(),
                extendSelection = get(),
            )
        }

        scopedOwner { EditorFeature(reducer = get()) }
    }
}