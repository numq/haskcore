package io.github.numq.haskcore.feature.workspace.core

import androidx.datastore.core.DataStoreFactory
import io.github.numq.haskcore.core.di.ScopePath
import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.workspace.core.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module
import java.nio.file.Files
import java.nio.file.Path

val workspaceCoreModule = module {
    scope<ScopeQualifier.Project> {
        scopedOwner {
            val projectPath = get<String>(qualifier = ScopePath.Project)

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            val dataStore = DataStoreFactory.create(
                serializer = WorkspaceDataSerializer, scope = scope, produceFile = {
                    Path.of(projectPath, ".haskcore").also(Files::createDirectories).resolve("workspace.pb").toFile()
                })

            LocalWorkspaceDataSource(scope = scope, dataStore = dataStore)
        } bind WorkspaceDataSource::class

        scopedOwner {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            LocalWorkspaceService(scope = scope, workspaceDataSource = get())
        } bind WorkspaceService::class

        scopedOwner { CloseWorkspaceDocument(projectService = get()) }

        scopedOwner {
            val projectPath = get<String>(qualifier = ScopePath.Project)

            CloseWorkspace(path = projectPath, sessionService = get())
        }

        scopedOwner { ObserveWorkspace(workspaceService = get(), documentService = get(), projectService = get()) }

        scopedOwner { OpenWorkspaceDocument(projectService = get()) }

        scopedOwner { SaveDimensions(workspaceService = get()) }

        scopedOwner { SaveRatio(workspaceService = get()) }
    }
}