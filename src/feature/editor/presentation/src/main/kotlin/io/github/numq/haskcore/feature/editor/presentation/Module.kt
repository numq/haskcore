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
        scopedOwner {
            MenuReducer(
                cutSelection = get(), copySelection = get(), pasteFromClipboard = get(), selectAll = get()
            )
        }

        scopedOwner {
            EditorReducer(
                menuReducer = get(),
                applyCodeSuggestion = get(),
                copySelection = get(),
                cutSelection = get(),
                extendSelection = get(),
                getCodeDocumentation = get(),
                getCodeSuggestions = get(),
                moveCaret = get(),
                observeAnalysis = get(),
                observeEditor = get(),
                observeSyntax = get(),
                pasteFromClipboard = get(),
                processKey = get(),
                saveEditorPosition = get(),
                selectAll = get(),
                startSelection = get(),
                updateActiveLines = get(),
                updateCollapsedLines = get(),
                updateFoldingRegions = get()
            )
        }

        scopedOwner { (path: String, language: Language) ->
            EditorFeature(path = path, language = language, reducer = get())
        }
    }
}