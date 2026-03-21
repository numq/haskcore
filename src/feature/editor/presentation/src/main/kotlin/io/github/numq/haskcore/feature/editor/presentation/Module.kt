package io.github.numq.haskcore.feature.editor.presentation

import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.editor.presentation.cache.PaintCache
import io.github.numq.haskcore.feature.editor.presentation.cache.ParagraphCache
import io.github.numq.haskcore.feature.editor.presentation.cache.TextLineCache
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorFeature
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorReducer
import io.github.numq.haskcore.feature.editor.presentation.layer.LayerFactory
import io.github.numq.haskcore.feature.editor.presentation.layer.SkiaLayerFactory
import org.koin.dsl.bind
import org.koin.dsl.module

val editorPresentationModule = module {
    scope<ScopeQualifierType.Application> {
        scopedOwner { TextLineCache(capacity = 1000) }

        scopedOwner { PaintCache(capacity = 1000) }

        scopedOwner { ParagraphCache(capacity = 1000) }

        scopedOwner {
            SkiaLayerFactory(textLineCache = get(), paintCache = get(), paragraphCache = get())
        } bind LayerFactory::class
    }

    scope<ScopeQualifierType.Document> {
        scopedOwner {
            EditorReducer(
                observeEditor = get(),
                updateActiveLines = get(),
                processKey = get(),
                moveCaret = get(),
                startSelection = get(),
                extendSelection = get(),
            )
        }

        scopedOwner { EditorFeature(reducer = get()) }
    }
}