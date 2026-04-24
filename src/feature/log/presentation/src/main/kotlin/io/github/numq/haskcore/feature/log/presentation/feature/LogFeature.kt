package io.github.numq.haskcore.feature.log.presentation.feature

import io.github.numq.haskcore.common.presentation.feature.Feature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class LogFeature(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()), reducer: LogReducer,
) : Feature<LogState, LogCommand, LogEvent> by Feature(
    initialState = LogState(), scope = scope, reducer = reducer, LogCommand.Initialize
)