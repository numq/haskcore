package io.github.numq.haskcore.feature.output.core

import androidx.datastore.core.DataStoreFactory
import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.feature.output.core.usecase.CloseOutputSession
import io.github.numq.haskcore.feature.output.core.usecase.CopySessionText
import io.github.numq.haskcore.feature.output.core.usecase.ObserveOutput
import io.github.numq.haskcore.feature.output.core.usecase.OpenOutputSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module
import java.nio.file.Files
import java.nio.file.Path

val outputFeatureCoreModule = module {
    scope<ScopeQualifier.Type.Project> {
        scopedOwner {
            val projectPath = get<String>(qualifier = ScopeQualifier.Project)

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            val dataStore = DataStoreFactory.create(
                serializer = OutputDataSerializer, scope = scope, produceFile = {
                    Path.of(projectPath, ".haskcore").also(Files::createDirectories).resolve("output.pb").toFile()
                })

            LocalOutputDataSource(scope = scope, dataStore = dataStore)
        } bind OutputDataSource::class

        scopedOwner {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            LocalOutputService(scope = scope, outputDataSource = get())
        } bind OutputService::class

        scopedOwner { CloseOutputSession(outputService = get()) }

        scopedOwner { CopySessionText(clipboardApi = get()) }

        scopedOwner { ObserveOutput(outputService = get(), runtimeApi = get()) }

        scopedOwner { OpenOutputSession(outputService = get()) }
    }
}