package io.github.numq.haskcore.application.presentation

import io.github.numq.haskcore.feature.Feature
import kotlinx.coroutines.*

internal class ApplicationFeature(
    private val feature: Feature<ApplicationCommand, ApplicationState>
) : Feature<ApplicationCommand, ApplicationState> by feature {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        coroutineScope.launch {
            execute(ApplicationCommand.Initialize)
        }
    }

    override val invokeOnClose: (suspend () -> Unit)? get() = { coroutineScope.cancel() }
}