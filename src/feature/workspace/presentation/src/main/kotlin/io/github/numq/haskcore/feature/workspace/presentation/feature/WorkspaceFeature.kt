package io.github.numq.haskcore.feature.workspace.presentation.feature

import io.github.numq.haskcore.common.presentation.feature.Feature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class WorkspaceFeature(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    reducer: WorkspaceReducer,
) : Feature<WorkspaceState, WorkspaceCommand, WorkspaceEvent> by Feature(
    initialState = WorkspaceState.Loading, scope = scope, reducer = reducer, WorkspaceCommand.Initialize
)