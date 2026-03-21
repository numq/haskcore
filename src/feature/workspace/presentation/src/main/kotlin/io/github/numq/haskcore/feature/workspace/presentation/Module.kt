package io.github.numq.haskcore.feature.workspace.presentation

import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.workspace.core.Workspace
import io.github.numq.haskcore.feature.workspace.presentation.feature.WorkspaceFeature
import io.github.numq.haskcore.feature.workspace.presentation.feature.WorkspaceReducer
import org.koin.dsl.module

val workspacePresentationModule = module {
    scope<ScopeQualifierType.Project> {
        scopedOwner {
            WorkspaceReducer(
                closeWorkspaceDocument = get(),
                closeWorkspace = get(),
                observeWorkspace = get(),
                openWorkspaceDocument = get(),
                saveDimensions = get(),
                saveRatio = get(),
            )
        }

        scopedOwner { (workspace: Workspace) ->
            WorkspaceFeature(workspace = workspace, reducer = get())
        }
    }
}