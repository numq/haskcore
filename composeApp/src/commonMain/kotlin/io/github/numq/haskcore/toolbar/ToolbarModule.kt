package io.github.numq.haskcore.toolbar

import io.github.numq.haskcore.feature.factory.CommandStrategy
import io.github.numq.haskcore.feature.factory.FeatureFactory
import io.github.numq.haskcore.toolbar.presentation.ToolbarFeature
import io.github.numq.haskcore.toolbar.presentation.ToolbarReducer
import io.github.numq.haskcore.toolbar.presentation.ToolbarState
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.dsl.module
import org.koin.dsl.onClose

@OptIn(DelicateCoroutinesApi::class)
internal val toolbarModule = module {
    single { (minimizeWindow: () -> Unit, toggleFullscreen: () -> Unit, exitApplication: () -> Unit) ->
        ToolbarFeature(
            minimizeWindow = minimizeWindow,
            toggleFullscreen = toggleFullscreen,
            exitApplication = exitApplication,
            feature = FeatureFactory().create(
                initialState = ToolbarState(), reducer = ToolbarReducer(
                    observeSession = get(), observeWorkspace = get(), openWorkspace = get(), closeWorkspace = get()
                ), strategy = CommandStrategy.Immediate
            )
        )
    } onClose { GlobalScope.launch { it?.close() } }
}