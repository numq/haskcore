package io.github.numq.haskcore.feature.execution.core

import androidx.datastore.core.DataStoreFactory
import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.execution.core.usecase.ObserveExecution
import io.github.numq.haskcore.feature.execution.core.usecase.SelectArtifact
import io.github.numq.haskcore.feature.execution.core.usecase.StartExecution
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module
import java.nio.file.Files
import java.nio.file.Path

val executionCoreModule = module {
    scope<ScopeQualifierType.Project> {
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

        scopedOwner {
            val projectPath = get<String>(qualifier = ScopeQualifier.Project)

            ObserveExecution(
                rootPath = projectPath, executionService = get(), toolchainService = get(), vfsService = get()
            )
        }

        scopedOwner { StartExecution(runtimeService = get()) }

        scopedOwner { SelectArtifact(executionService = get()) }
    }
}