package io.github.numq.haskcore.feature.explorer.presentation

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.core.feature.Feature
import io.github.numq.haskcore.feature.explorer.core.ExplorerRoot
import io.github.numq.haskcore.feature.explorer.core.ExplorerTree
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerCommand
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerEvent
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerReducer
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

val explorerPresentationModule = module {
    scope<ScopeQualifierType.Project> {
        scopedOwner {
            val projectPath = get<String>(qualifier = ScopeQualifier.Project)

            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            val reducer = ExplorerReducer(
                expandDirectory = get(),
                collapseDirectory = get(),
                observeExplorerTree = get(),
                saveExplorerPosition = get(),
                openFile = get()
            )

            Feature<ExplorerState, ExplorerCommand, ExplorerEvent>(
                initialState = ExplorerState(explorerTree = ExplorerTree.Loading(root = ExplorerRoot(path = projectPath))),
                scope = scope,
                reducer = reducer,
                ExplorerCommand.Initialize
            )
        }
    }
}