package io.github.numq.haskcore.feature.status.presentation

import io.github.numq.haskcore.core.feature.Feature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class StatusFeature(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()), reducer: StatusReducer
) : Feature<StatusState, StatusCommand, StatusEvent> by Feature(
    initialState = StatusState(), scope = scope, reducer = reducer, StatusCommand.Initialize
)