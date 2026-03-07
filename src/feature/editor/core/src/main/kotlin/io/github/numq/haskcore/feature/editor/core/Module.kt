package io.github.numq.haskcore.feature.editor.core

import io.github.numq.haskcore.core.di.ScopePath
import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
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

val editorCoreModule = module {
    scope<ScopeQualifier.Document> {
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

        scopedOwner { ExtendSelection(editorService = get(), textService = get()) }

        scopedOwner { MoveCaret(editorService = get(), textService = get()) }

        scopedOwner { ObserveCaret(editorService = get()) }

        scopedOwner { ObserveHighlighting(editorService = get(), textService = get()) }

        scopedOwner { ObserveOccurrences(editorService = get(), textService = get()) }

        scopedOwner { ObserveSelection(editorService = get()) }

        scopedOwner {
            val documentPath = get<String>(qualifier = ScopePath.Document)

            ObserveTextSnapshot(
                path = documentPath,
                editorService = get(),
                documentService = get(),
                journalService = get(),
                textService = get(),
                vfsService = get()
            )
        }

        scopedOwner {
            val documentPath = get<String>(qualifier = ScopePath.Document)

            ProcessKey(
                path = documentPath,
                editorService = get(),
                documentService = get(),
                journalService = get(),
                keymapService = get(),
                textService = get()
            )
        }

        scopedOwner { RequestHighlightingUpdate(editorService = get()) }

        scopedOwner { StartSelection(editorService = get(), textService = get()) }
    }
}