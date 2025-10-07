package io.github.numq.haskcore.explorer.presentation.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.github.numq.haskcore.explorer.presentation.ExplorerFeature
import io.github.numq.haskcore.explorer.presentation.ExplorerState

@Composable
internal fun ExplorerView(feature: ExplorerFeature) {
    when (val state = feature.state.collectAsState().value) {
        is ExplorerState.Empty -> ExplorerViewEmpty(feature = feature, state = state)

        is ExplorerState.Loading -> ExplorerViewLoading(feature = feature, state = state)

        is ExplorerState.Loaded -> ExplorerViewLoaded(feature = feature, state = state)

        is ExplorerState.Failure -> ExplorerViewFailure(feature = feature, state = state)
    }
}