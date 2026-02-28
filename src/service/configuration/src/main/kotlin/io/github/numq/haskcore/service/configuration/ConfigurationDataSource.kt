package io.github.numq.haskcore.service.configuration

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

internal interface ConfigurationDataSource : AutoCloseable {
    val configurationData: Flow<ConfigurationData>

    suspend fun get(): Either<Throwable, ConfigurationData>

    suspend fun update(transform: (ConfigurationData) -> ConfigurationData): Either<Throwable, ConfigurationData>
}