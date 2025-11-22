package io.github.numq.haskcore.configuration.presentation

import io.github.numq.haskcore.configuration.Configuration
import io.github.numq.haskcore.feature.Event
import kotlinx.coroutines.flow.Flow

internal sealed interface ConfigurationEvent {
    data class ObserveConfigurations(
        override val flow: Flow<List<Configuration>>,
    ) : ConfigurationEvent, Event.Collectable<List<Configuration>>() {
        override val key = ConfigurationEventKey.OBSERVE_CONFIGURATIONS
    }
}