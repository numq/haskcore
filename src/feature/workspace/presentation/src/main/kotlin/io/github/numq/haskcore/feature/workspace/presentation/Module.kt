package io.github.numq.haskcore.feature.workspace.presentation

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.workspace.core.Workspace
import org.koin.dsl.module

val workspacePresentationModule = module {
    scope<ScopeQualifier.Project> {
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