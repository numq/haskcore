package io.github.numq.haskcore.service.configuration

import androidx.datastore.core.DataStore
import arrow.core.Either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first

internal class LocalConfigurationDataSource(
    private val scope: CoroutineScope, private val dataStore: DataStore<ConfigurationData>
) : ConfigurationDataSource {
    override val configurationData = dataStore.data

    override suspend fun get() = Either.catch { dataStore.data.first() }

    override suspend fun update(transform: (ConfigurationData) -> ConfigurationData) = Either.catch {
        dataStore.updateData(transform)
    }

    override fun close() {
        scope.cancel()
    }
}