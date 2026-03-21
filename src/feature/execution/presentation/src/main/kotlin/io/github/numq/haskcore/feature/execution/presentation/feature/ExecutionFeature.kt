package io.github.numq.haskcore.feature.execution.presentation.feature

import io.github.numq.haskcore.core.feature.Feature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class ExecutionFeature(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()), reducer: ExecutionReducer
) : Feature<ExecutionState, ExecutionCommand, ExecutionEvent> by Feature(
    initialState = ExecutionState(), scope = scope, reducer = reducer, ExecutionCommand.ObserveExecution
)