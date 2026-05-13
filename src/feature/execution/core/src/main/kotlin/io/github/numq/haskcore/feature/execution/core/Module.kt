package io.github.numq.haskcore.feature.execution.core

import androidx.datastore.core.DataStoreFactory
import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.feature.execution.core.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module
import java.nio.file.Files
import java.nio.file.Path

val executionFeatureCoreModule = module {
    scope<ScopeQualifier.Type.Project> {
        scopedOwner {
            val projectPath = get<String>(qualifier = ScopeQualifier.Project)

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            val dataStore = DataStoreFactory.create(
                serializer = ExecutionDataSerializer, scope = scope, produceFile = {
                    Path.of(projectPath, ".haskcore").also(Files::createDirectories).resolve("execution.pb").toFile()
                })

            LocalExecutionDataSource(scope = scope, dataStore = dataStore)
        } bind ExecutionDataSource::class

        scopedOwner {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            LocalExecutionService(scope = scope, executionDataSource = get())
        } bind ExecutionService::class

        scopedOwner { BuildConfiguration(runtimeService = get()) }

        scopedOwner { DeleteConfiguration(executionService = get()) }

        scopedOwner { EditConfiguration(executionService = get()) }

        scopedOwner {
            val projectPath = get<String>(qualifier = ScopeQualifier.Project)

            ObserveExecution(
                rootPath = projectPath,
                documentService = get(),
                executionService = get(),
                runtimeService = get(),
                toolchainService = get(),
                vfsService = get()
            )
        }

        scopedOwner { RunConfiguration(runtimeService = get()) }

        scopedOwner { SetCurrentConfiguration(executionService = get()) }

        scopedOwner { StopConfiguration(runtimeService = get()) }
    }
}