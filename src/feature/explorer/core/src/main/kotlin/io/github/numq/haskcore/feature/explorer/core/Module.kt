package io.github.numq.haskcore.feature.explorer.core

import androidx.datastore.core.DataStoreFactory
import io.github.numq.haskcore.core.di.ScopePath
import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.explorer.core.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module
import java.nio.file.Files
import java.nio.file.Path

val explorerCoreModule = module {
    scope<ScopeQualifier.Project> {
        scopedOwner {
            val projectPath = get<String>(qualifier = ScopePath.Project)

            ExplorerRoot(path = projectPath)
        }

        scopedOwner {
            val projectPath = get<String>(qualifier = ScopePath.Project)

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            val dataStore = DataStoreFactory.create(serializer = ExplorerDataSerializer, scope = scope, produceFile = {
                Path.of(projectPath, ".haskcore").also(Files::createDirectories).resolve("explorer.pb").toFile()
            })

            LocalExplorerDataSource(scope = scope, dataStore = dataStore)
        } bind ExplorerDataSource::class

        scopedOwner {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            LocalExplorerService(scope = scope, explorerDataSource = get())
        } bind ExplorerService::class

        scopedOwner { ObserveExplorerTree(root = get<ExplorerRoot>(), explorerService = get(), vfsService = get()) }

        scopedOwner { OpenFile(projectService = get()) }

        scopedOwner { SaveExplorerPosition(explorerService = get()) }

        scopedOwner { SelectExplorerNode(explorerService = get()) }

        scopedOwner { ToggleExplorerNode(explorerService = get()) }
    }
}