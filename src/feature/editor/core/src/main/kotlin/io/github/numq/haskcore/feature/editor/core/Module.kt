package io.github.numq.haskcore.feature.editor.core

import androidx.datastore.core.DataStoreFactory
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
import java.nio.file.Files
import java.nio.file.Path

val editorFeatureCoreModule = module {
    scope<ScopeQualifier.Type.Project> {
        scopedOwner {
            val projectPath = get<String>(qualifier = ScopeQualifier.Project)

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            val dataStore = DataStoreFactory.create(serializer = EditorDataSerializer, scope = scope, produceFile = {
                Path.of(projectPath, ".haskcore").also(Files::createDirectories).resolve("editor.pb").toFile()
            })

            LocalEditorDataSource(scope = scope, dataStore = dataStore)
        } bind EditorDataSource::class
    }

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

            LocalEditorService(scope = scope, caretManager = get(), selectionManager = get(), editorDataSource = get())
        } bind EditorService::class

        scopedOwner { ApplyCodeSuggestion(editorService = get(), textService = get()) }

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

            GetCodeDocumentation(path = documentPath, lspService = get())
        }

        scopedOwner {
            val documentPath = get<String>(qualifier = ScopeQualifier.Document)

            GetCodeSuggestions(path = documentPath, lspService = get())
        }

        scopedOwner { UpdateActiveLines(editorService = get()) }

        scopedOwner { SaveEditorPosition(editorService = get()) }

        scopedOwner { StartSelection(editorService = get(), textService = get()) }
    }
}