package io.github.numq.haskcore.feature.welcome.presentation.feature

import io.github.numq.haskcore.core.feature.Feature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class WelcomeFeature(
    title: String,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    reducer: WelcomeReducer
) : Feature<WelcomeState, WelcomeCommand, WelcomeEvent> by Feature(
    initialState = WelcomeState(title = title), scope = scope, reducer = reducer, WelcomeCommand.Initialize
)