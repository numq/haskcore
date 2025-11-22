package io.github.numq.haskcore.configuration

import kotlinx.coroutines.flow.Flow

internal interface ConfigurationRepository {
    val configurations: Flow<List<Configuration>>

    suspend fun addConfiguration(configuration: Configuration): Result<Configuration>

    suspend fun editConfiguration(configuration: Configuration): Result<Configuration>

    suspend fun removeConfiguration(configuration: Configuration): Result<Unit>

    class Default(private val configurationDataSource: ConfigurationDataSource) : ConfigurationRepository {
        override val configurations = configurationDataSource.configurations

        override suspend fun addConfiguration(configuration: Configuration) =
            configurationDataSource.create(configuration = configuration)

        override suspend fun editConfiguration(configuration: Configuration) =
            configurationDataSource.update(configuration = configuration)

        override suspend fun removeConfiguration(configuration: Configuration) =
            configurationDataSource.delete(configuration = configuration)
    }
}