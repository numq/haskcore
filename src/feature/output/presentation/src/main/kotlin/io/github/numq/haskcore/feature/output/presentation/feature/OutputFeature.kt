package io.github.numq.haskcore.feature.output.presentation.feature

import io.github.numq.haskcore.common.presentation.feature.Feature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class OutputFeature(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()), reducer: OutputReducer,
) : Feature<OutputState, OutputCommand, OutputEvent> by Feature(
    initialState = OutputState(), scope = scope, reducer = reducer, OutputCommand.Initialize
)