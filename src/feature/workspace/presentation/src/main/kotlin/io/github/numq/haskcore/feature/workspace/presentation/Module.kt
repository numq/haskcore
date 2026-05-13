package io.github.numq.haskcore.feature.workspace.presentation

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.feature.workspace.presentation.feature.WorkspaceFeature
import io.github.numq.haskcore.feature.workspace.presentation.feature.WorkspaceReducer
import org.koin.dsl.module

val workspaceFeaturePresentationModule = module {
    scope<ScopeQualifier.Type.Project> {
        scopedOwner {
            WorkspaceReducer(
                closeWorkspaceDocument = get(),
                closeWorkspace = get(),
                observeWorkspace = get(),
                openWorkspaceDocument = get(),
                selectShelfTool = get(),
                saveLeftShelfPanelRatio = get(),
                saveRightShelfPanelRatio = get(),
                saveVerticalRatio = get(),
                saveDimensions = get(),
            )
        }

        scopedOwner { WorkspaceFeature(reducer = get()) }
    }
}