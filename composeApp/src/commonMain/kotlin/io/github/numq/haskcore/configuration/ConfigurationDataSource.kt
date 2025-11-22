package io.github.numq.haskcore.configuration

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal interface ConfigurationDataSource {
    val configurations: Flow<List<Configuration>>

    suspend fun create(configuration: Configuration): Result<Configuration>

    suspend fun update(configuration: Configuration): Result<Configuration>

    suspend fun delete(configuration: Configuration): Result<Unit>

    class Default(private val dataStore: DataStore<ConfigurationList>) : ConfigurationDataSource {
        override val configurations = dataStore.data.map { configurationList ->
            configurationList.configurations
        }

        override suspend fun create(configuration: Configuration) = runCatching {
            dataStore.updateData { configurationList ->
                configurationList.copy(configurations = configurationList.configurations + configuration)
            }

            configuration
        }

        override suspend fun update(configuration: Configuration) = runCatching {
            dataStore.updateData { configurationList ->
                configurationList.copy(configurations = configurationList.configurations.map { thisConfiguration ->
                    when (thisConfiguration.id) {
                        configuration.id -> configuration

                        else -> thisConfiguration
                    }
                })
            }

            configuration
        }

        override suspend fun delete(configuration: Configuration) = runCatching {
            dataStore.updateData { configurationList ->
                configurationList.copy(configurations = configurationList.configurations.filterNot { thisConfiguration ->
                    thisConfiguration.id == configuration.id
                })
            }

            Unit
        }
    }
}