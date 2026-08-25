package io.github.numq.haskcore.feature.explorer.presentation.feature

import io.github.numq.haskcore.common.presentation.feature.Feature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class ExplorerFeature(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()), reducer: ExplorerReducer,
) : Feature<ExplorerState, ExplorerCommand, ExplorerEvent> by Feature(
    initialState = ExplorerState.Loading, scope = scope, reducer = reducer, ExplorerCommand.Initialize
)