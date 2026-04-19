package io.github.numq.haskcore.feature.bootstrap.presentation.feature

import io.github.numq.haskcore.common.presentation.feature.Feature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class BootstrapFeature(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()), reducer: BootstrapReducer,
) : Feature<BootstrapState, BootstrapCommand, BootstrapEvent> by Feature(
    initialState = BootstrapState.Active, scope = scope, reducer = reducer, BootstrapCommand.Initialize
)