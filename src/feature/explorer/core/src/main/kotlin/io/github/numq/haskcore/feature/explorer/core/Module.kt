package io.github.numq.haskcore.feature.explorer.core

import androidx.datastore.core.DataStoreFactory
import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.feature.explorer.core.usecase.CollapseDirectory
import io.github.numq.haskcore.feature.explorer.core.usecase.CreateDirectoryNode
import io.github.numq.haskcore.feature.explorer.core.usecase.CreateFileNode
import io.github.numq.haskcore.feature.explorer.core.usecase.ExpandDirectory
import io.github.numq.haskcore.feature.explorer.core.usecase.ObserveExplorerTree
import io.github.numq.haskcore.feature.explorer.core.usecase.OpenFile
import io.github.numq.haskcore.feature.explorer.core.usecase.SaveExplorerPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module
import java.nio.file.Files
import java.nio.file.Path

val explorerFeatureCoreModule = module {
    scope<ScopeQualifier.Type.Project> {
        scopedOwner {
            val projectPath = get<String>(qualifier = ScopeQualifier.Project)

            ExplorerRoot(path = projectPath)
        }

        scopedOwner {
            val projectPath = get<String>(qualifier = ScopeQualifier.Project)

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            val dataStore = DataStoreFactory.create(
                serializer = ExplorerDataSerializer,
                scope = scope,
                produceFile = {
                    Path.of(projectPath, ".haskcore").also(Files::createDirectories)
                        .resolve("explorer.pb").toFile()
                })

            LocalExplorerDataSource(scope = scope, dataStore = dataStore)
        } bind ExplorerDataSource::class

        scopedOwner {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            LocalExplorerService(scope = scope, explorerDataSource = get())
        } bind ExplorerService::class

        scopedOwner {
            ObserveExplorerTree(
                root = get<ExplorerRoot>(),
                explorerService = get(),
                vfsService = get()
            )
        }

        scopedOwner { CollapseDirectory(explorerService = get()) }

        scopedOwner { CreateDirectoryNode(vfsService = get()) }

        scopedOwner { CreateFileNode(vfsService = get()) }

        scopedOwner { ExpandDirectory(explorerService = get()) }

        scopedOwner { OpenFile(projectService = get()) }

        scopedOwner { SaveExplorerPosition(explorerService = get()) }
    }
}