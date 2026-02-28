package io.github.numq.haskcore.service.configuration

import io.github.numq.haskcore.core.timestamp.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class LocalConfigurationService(
    private val scope: CoroutineScope, configurationDataSource: ConfigurationDataSource
) : ConfigurationService {
    override val configuration = configurationDataSource.configurationData.map(ConfigurationData::toConfiguration).stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = Configuration(timestamp = Timestamp(nanoseconds = 0L))
    )

    override fun close() {
        scope.cancel()
    }
}