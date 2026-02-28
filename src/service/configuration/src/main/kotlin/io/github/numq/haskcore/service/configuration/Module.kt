package io.github.numq.haskcore.service.configuration

import androidx.datastore.core.DataStoreFactory
import io.github.numq.haskcore.core.di.ScopePath
import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module
import java.nio.file.Files
import java.nio.file.Path

val configurationModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner {
            val applicationPath = get<String>(qualifier = ScopePath.Application)

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            val dataStore = DataStoreFactory.create(
                serializer = ConfigurationDataSerializer, scope = scope, produceFile = {
                    Path.of(applicationPath, ".haskcore").also(Files::createDirectories).resolve("configuration.pb").toFile()
                })

            LocalConfigurationDataSource(scope = scope, dataStore = dataStore)
        } bind ConfigurationDataSource::class

        scopedOwner {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            LocalConfigurationService(scope = scope, configurationDataSource = get())
        } bind ConfigurationService::class
    }
}