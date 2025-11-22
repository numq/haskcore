package io.github.numq.haskcore.configuration

import androidx.datastore.core.DataStoreFactory
import io.github.numq.haskcore.configuration.presentation.ConfigurationFeature
import io.github.numq.haskcore.configuration.presentation.ConfigurationReducer
import io.github.numq.haskcore.configuration.presentation.ConfigurationState
import io.github.numq.haskcore.configuration.usecase.*
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
internal val configurationModule = module {
    scope(WorkspaceScopeQualifier) {
        scoped {
            val workspacePath = get<String>()

            val dataStore = DataStoreFactory.create(serializer = ConfigurationSerializer, produceFile = {
                Path.of(workspacePath, ".haskcore").also { dir ->
                    Files.createDirectories(dir)
                }.resolve("configurations.pb").toFile()
            })

            ConfigurationDataSource.Default(dataStore = dataStore)
        } bind ConfigurationDataSource::class

        scoped { ConfigurationRepository.Default(configurationDataSource = get()) } bind ConfigurationRepository::class

        scoped { AddConfiguration(configurationRepository = get()) }

        scoped { EditConfiguration(configurationRepository = get()) }

        scoped { ObserveConfigurations(configurationRepository = get()) }

        scoped { RemoveConfiguration(configurationRepository = get()) }

        scoped { RunConfiguration(buildSystemRepository = get(), outputRepository = get()) }

        scoped {
            ConfigurationFeature(
                feature = FeatureFactory().create(
                    initialState = ConfigurationState(), reducer = ConfigurationReducer(
                        addConfiguration = get(),
                        editConfiguration = get(),
                        observeConfigurations = get(),
                        removeConfiguration = get()
                    ), strategy = CommandStrategy.Immediate
                )
            )
        } onClose { GlobalScope.launch { it?.close() } }
    }
}