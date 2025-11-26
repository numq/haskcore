package io.github.numq.haskcore.explorer

import androidx.datastore.core.DataStoreFactory
import io.github.numq.haskcore.explorer.presentation.ExplorerFeature
import io.github.numq.haskcore.explorer.presentation.ExplorerState
import io.github.numq.haskcore.explorer.presentation.reducer.ExplorerReducer
import io.github.numq.haskcore.explorer.presentation.reducer.ExplorerSelectionReducer
import io.github.numq.haskcore.explorer.usecase.*
import io.github.numq.haskcore.feature.factory.CommandStrategy
import io.github.numq.haskcore.feature.factory.FeatureFactory
import io.github.numq.haskcore.workspace.WorkspaceScopeQualifier
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose
import java.nio.file.Files
import java.nio.file.Path

@OptIn(DelicateCoroutinesApi::class)
internal val explorerModule = module {
    scope(WorkspaceScopeQualifier) {
        scoped {
            val workspacePath = get<String>()

            val dataStore = DataStoreFactory.create(serializer = ExplorerSnapshotSerializer, produceFile = {
                Path.of(workspacePath, ".haskcore").also { dir ->
                    Files.createDirectories(dir)
                }.resolve("explorer.pb").toFile()
            })

            ExplorerSnapshotDataSource.Default(dataStore = dataStore)
        } bind ExplorerSnapshotDataSource::class

        scoped {
            val workspacePath = get<String>()

            ExplorerRepository.Default(rootPath = workspacePath, explorerSnapshotDataSource = get())
        } bind ExplorerRepository::class

        scoped { CollapseExplorerNode(explorerRepository = get()) }

        scoped { CopyExplorerNodes(clipboardRepository = get()) }

        scoped { CreateExplorerNode(explorerRepository = get()) }

        scoped { CutExplorerNodes(explorerRepository = get()) }

        scoped { DeleteExplorerNodes(explorerRepository = get()) }

        scoped { ExpandExplorerNode(explorerRepository = get()) }

        scoped { MoveExplorerNodes(explorerRepository = get()) }

        scoped { ObserveExplorer(explorerRepository = get()) }

        scoped { PasteExplorerNodes(explorerRepository = get()) }

        scoped { RenameExplorerNode(explorerRepository = get()) }

        scoped {
            ExplorerFeature(
                feature = FeatureFactory().create(
                    initialState = ExplorerState(), reducer = ExplorerReducer(
                        explorerSelectionReducer = ExplorerSelectionReducer(),
                        observeExplorer = get(),
                        collapseExplorerNode = get(),
                        copyExplorerNodes = get(),
                        createExplorerNode = get(),
                        cutExplorerNodes = get(),
                        deleteExplorerNodes = get(),
                        expandExplorerNode = get(),
                        moveExplorerNodes = get(),
                        pasteExplorerNodes = get(),
                        renameExplorerNode = get(),
                        openDocument = get()
                    ), strategy = CommandStrategy.Immediate
                )
            )
        } onClose { GlobalScope.launch { it?.close() } }
    }
}