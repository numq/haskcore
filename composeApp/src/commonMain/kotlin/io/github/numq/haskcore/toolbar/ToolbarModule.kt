package io.github.numq.haskcore.toolbar

import io.github.numq.haskcore.feature.factory.CommandStrategy
import io.github.numq.haskcore.feature.factory.FeatureFactory
import io.github.numq.haskcore.toolbar.presentation.ToolbarFeature
import io.github.numq.haskcore.toolbar.presentation.ToolbarReducer
import io.github.numq.haskcore.toolbar.presentation.ToolbarState
import io.github.numq.haskcore.workspace.Workspace
import org.koin.dsl.module

internal val toolbarModule = module {
    single { (minimizeWindow: () -> Unit, toggleFullscreen: () -> Unit, exitApplication: () -> Unit, activeWorkspace: Workspace?, recentWorkspaces: List<Workspace>) ->
        ToolbarFeature(
            minimizeWindow = minimizeWindow,
            toggleFullscreen = toggleFullscreen,
            exitApplication = exitApplication,
            feature = FeatureFactory().create(
                initialState = ToolbarState(activeWorkspace = activeWorkspace, recentWorkspaces = recentWorkspaces),
                reducer = ToolbarReducer(
                    observeRecentWorkspaces = get(),
                    observeWorkspace = get(),
                    openWorkspace = get(),
                    closeWorkspace = get()
                ),
                strategy = CommandStrategy.Immediate
            )
        )
    }
}