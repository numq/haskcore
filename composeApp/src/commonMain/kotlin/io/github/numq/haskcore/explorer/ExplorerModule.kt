package io.github.numq.haskcore.explorer

import io.github.numq.haskcore.explorer.usecase.*
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose

internal val explorerModule = module {
    single { ExplorerDataSource.Default() } bind ExplorerDataSource::class

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

    single { PasteExplorerNodes(explorerRepository = get()) }

    single { RenameExplorerNode(explorerRepository = get()) }
}