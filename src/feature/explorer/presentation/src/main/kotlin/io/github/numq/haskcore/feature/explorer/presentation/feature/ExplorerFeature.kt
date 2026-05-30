package io.github.numq.haskcore.feature.explorer.presentation.feature

import io.github.numq.haskcore.common.presentation.feature.Feature
import io.github.numq.haskcore.feature.explorer.core.ExplorerRoot
import io.github.numq.haskcore.feature.explorer.core.ExplorerTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ExplorerFeature(
    private val path: String,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()), reducer: ExplorerReducer,
) : Feature<ExplorerState, ExplorerCommand, ExplorerEvent> by Feature(
    initialState = ExplorerState(explorerTree = ExplorerTree.Loading(root = ExplorerRoot(path = path))),
    scope = scope,
    reducer = reducer,
    ExplorerCommand.Initialize
)