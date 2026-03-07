package io.github.numq.haskcore.feature.explorer.presentation

import io.github.numq.haskcore.core.feature.Feature
import io.github.numq.haskcore.feature.explorer.core.ExplorerRoot
import io.github.numq.haskcore.feature.explorer.core.ExplorerTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class ExplorerFeature(
    path: String,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    reducer: ExplorerReducer
) : Feature<ExplorerState, ExplorerCommand, ExplorerEvent> by Feature(
    initialState = ExplorerState(explorerTree = ExplorerTree.Loading(root = ExplorerRoot(path = path))),
    scope = scope,
    reducer = reducer,
    ExplorerCommand.Initialize
)