package io.github.numq.haskcore.feature.workspace.presentation.feature

import io.github.numq.haskcore.core.feature.Feature
import io.github.numq.haskcore.feature.workspace.core.Workspace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class WorkspaceFeature(
    workspace: Workspace,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    reducer: WorkspaceReducer
) : Feature<WorkspaceState, WorkspaceCommand, WorkspaceEvent> by Feature(
    initialState = WorkspaceState(workspace = workspace), scope = scope, reducer = reducer, WorkspaceCommand.Initialize
)