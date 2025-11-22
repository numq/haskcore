package io.github.numq.haskcore.configuration.presentation

import io.github.numq.haskcore.feature.Feature
import kotlinx.coroutines.*

internal class ConfigurationFeature(
    private val feature: Feature<ConfigurationCommand, ConfigurationState>
) : Feature<ConfigurationCommand, ConfigurationState> by feature {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        coroutineScope.launch {
            events.collect { event ->
                when (event) {
                    is ConfigurationEvent.ObserveConfigurations -> collect(
                        event = event, joinCancellation = false, action = { configurations ->
                            execute(ConfigurationCommand.UpdateConfigurations(configurations = configurations))
                        })

                    else -> {
                        // todo

                        println(event)
                    }
                }
            }
        }

        coroutineScope.launch {
            execute(ConfigurationCommand.Initialize)
        }
    }

    override val invokeOnClose: (suspend () -> Unit)? get() = { coroutineScope.cancel() }
}