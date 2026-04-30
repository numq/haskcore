package io.github.numq.haskcore.feature.navigation.presentation.feature

import io.github.numq.haskcore.common.presentation.feature.Feature
import io.github.numq.haskcore.feature.navigation.core.Destination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class NavigationFeature(
    initialDestinations: List<Destination>,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    reducer: NavigationReducer,
) : Feature<NavigationState, NavigationCommand, NavigationEvent> by Feature(
    initialState = NavigationState(destinations = initialDestinations),
    scope = scope,
    reducer = reducer,
    NavigationCommand.Initialize
)