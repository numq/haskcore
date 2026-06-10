package io.github.numq.haskcore.feature.editor.presentation

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.common.core.language.Language
import io.github.numq.haskcore.feature.editor.presentation.cache.PaintCache
import io.github.numq.haskcore.feature.editor.presentation.cache.ParagraphCache
import io.github.numq.haskcore.feature.editor.presentation.cache.TextLineCache
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorFeature
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorReducer
import io.github.numq.haskcore.feature.editor.presentation.layer.LayerFactory
import io.github.numq.haskcore.feature.editor.presentation.layer.SkiaLayerFactory
import io.github.numq.haskcore.feature.editor.presentation.menu.MenuReducer
import org.koin.dsl.bind
import org.koin.dsl.module

val editorFeaturePresentationModule = module {
    scope<ScopeQualifier.Type.Application> {
        scopedOwner { TextLineCache(capacity = 1000) }

        scopedOwner { PaintCache(capacity = 1000) }

        scopedOwner { ParagraphCache(capacity = 1000) }

        scopedOwner {
            SkiaLayerFactory(textLineCache = get(), paintCache = get(), paragraphCache = get())
        } bind LayerFactory::class
    }

    scope<ScopeQualifier.Type.Document> {
        scopedOwner { MenuReducer() }

        scopedOwner {
            EditorReducer(
                menuReducer = get(),
                observeAnalysis = get(),
                observeEditor = get(),
                observeSyntax = get(),
                updateActiveLines = get(),
                getCodeDocumentation = get(),
                getCodeSuggestions = get(),
                applyCodeSuggestion = get(),
                processKey = get(),
                moveCaret = get(),
                saveEditorPosition = get(),
                startSelection = get(),
                extendSelection = get()
            )
        }

        scopedOwner { (path: String, language: Language) ->
            EditorFeature(path = path, language = language, reducer = get())
        }
    }
}