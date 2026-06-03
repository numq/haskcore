package io.github.numq.haskcore.feature.editor.core

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.feature.editor.core.caret.CaretManager
import io.github.numq.haskcore.feature.editor.core.caret.DefaultCaretManager
import io.github.numq.haskcore.feature.editor.core.selection.DefaultSelectionManager
import io.github.numq.haskcore.feature.editor.core.selection.SelectionManager
import io.github.numq.haskcore.feature.editor.core.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module

val editorFeatureCoreModule = module {
    scope<ScopeQualifier.Type.Document> {
        scopedOwner {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            DefaultCaretManager(scope = scope)
        } bind CaretManager::class

        scopedOwner {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            DefaultSelectionManager(scope = scope)
        } bind SelectionManager::class

        scopedOwner {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            LocalEditorService(scope = scope, caretManager = get(), selectionManager = get())
        } bind EditorService::class

        scopedOwner { ApplyCodeSuggestion(editorService = get(), textService = get()) }

        scopedOwner {
            val documentPath = get<String>(qualifier = ScopeQualifier.Document)

            DismissCodeDocumentation(path = documentPath, lspService = get())
        }

        scopedOwner { ExtendSelection(editorService = get(), textService = get()) }

        scopedOwner { MoveCaret(editorService = get(), textService = get()) }

        scopedOwner {
            ObserveAnalysis(editorService = get(), loggerService = get(), lspService = get(), textService = get())
        }

        scopedOwner {
            ObserveEditor(
                editorService = get(),
                documentService = get(),
                journalService = get(),
                loggerService = get(),
                textService = get(),
                vfsService = get()
            )
        }

        scopedOwner {
            ObserveSyntax(editorService = get(), syntaxService = get(), loggerService = get(), textService = get())
        }

        scopedOwner {
            val documentPath = get<String>(qualifier = ScopeQualifier.Document)

            ProcessKey(
                path = documentPath,
                editorService = get(),
                clipboardService = get(),
                documentService = get(),
                journalService = get(),
                keymapService = get(),
                textService = get()
            )
        }

        scopedOwner {
            val documentPath = get<String>(qualifier = ScopeQualifier.Document)

            RequestCodeDocumentation(path = documentPath, lspService = get())
        }

        scopedOwner { UpdateActiveLines(editorService = get()) }

        scopedOwner { StartSelection(editorService = get(), textService = get()) }
    }
}