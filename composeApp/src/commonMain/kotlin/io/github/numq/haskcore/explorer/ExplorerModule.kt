package io.github.numq.haskcore.explorer

import io.github.numq.haskcore.explorer.presentation.ExplorerFeature
import io.github.numq.haskcore.explorer.presentation.ExplorerState
import io.github.numq.haskcore.explorer.presentation.reducer.*
import io.github.numq.haskcore.explorer.usecase.*
import io.github.numq.haskcore.feature.factory.CommandStrategy
import io.github.numq.haskcore.feature.factory.FeatureFactory
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose

internal val explorerModule = module {
    single { ExplorerDataSource() }

    single {
        ExplorerRepository.Default(
            explorerDataSource = get(), clipboardRepository = get(), fileSystemService = get()
        )
    } bind ExplorerRepository::class onClose { it?.close() }

    single { CollapseExplorerNode(explorerRepository = get()) }

    single { CopyExplorerNodes(explorerRepository = get()) }

    single { CreateExplorerNode(explorerRepository = get()) }

    single { CutExplorerNodes(explorerRepository = get()) }

    single { DeleteExplorerNodes(explorerRepository = get()) }

    single { ExpandExplorerNode(explorerRepository = get()) }

    single { GetExplorer(explorerRepository = get()) }

    single { MoveExplorerNodes(explorerRepository = get()) }

    single { OpenExplorer(explorerRepository = get()) }

    single { PasteExplorerNodes(explorerRepository = get()) }

    single { RenameExplorerNode(explorerRepository = get()) }

    single { (path: String) ->
        ExplorerFeature(
            path = path, feature = FeatureFactory().create(
                initialState = ExplorerState(), reducer = ExplorerReducer(
                    getExplorer = get(),
                    openExplorer = get(),
                    explorerContextMenuReducer = ExplorerContextMenuReducer(),
                    explorerDialogReducer = ExplorerDialogReducer(),
                    explorerOperationReducer = ExplorerOperationReducer(
                        collapseExplorerNode = get(),
                        copyExplorerNodes = get(),
                        createExplorerNode = get(),
                        cutExplorerNodes = get(),
                        deleteExplorerNodes = get(),
                        expandExplorerNode = get(),
                        moveExplorerNodes = get(),
                        pasteExplorerNodes = get(),
                        renameExplorerNode = get(),
                        openDocument = get(),
                    ),
                    explorerSelectionReducer = ExplorerSelectionReducer(),
                ), strategy = CommandStrategy.Immediate
            )
        )
    }
}