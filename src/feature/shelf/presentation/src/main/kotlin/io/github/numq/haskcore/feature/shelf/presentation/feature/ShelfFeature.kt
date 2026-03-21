package io.github.numq.haskcore.feature.shelf.presentation.feature

import io.github.numq.haskcore.core.feature.Feature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class ShelfFeature(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()), reducer: ShelfReducer
) : Feature<ShelfState, ShelfCommand, ShelfEvent> by Feature(
    initialState = ShelfState(), scope = scope, reducer = reducer, ShelfCommand.Initialize
)