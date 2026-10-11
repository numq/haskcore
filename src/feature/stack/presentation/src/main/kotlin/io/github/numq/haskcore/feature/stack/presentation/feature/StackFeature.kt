package io.github.numq.haskcore.feature.stack.presentation.feature

import io.github.numq.haskcore.common.presentation.feature.Feature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class StackFeature(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()), reducer: StackReducer,
) : Feature<StackState, StackCommand, StackEvent> by Feature(
    initialState = StackState(), scope = scope, reducer = reducer, StackCommand.Initialize
)