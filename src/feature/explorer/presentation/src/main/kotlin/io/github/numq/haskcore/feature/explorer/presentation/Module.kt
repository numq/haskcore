package io.github.numq.haskcore.feature.explorer.presentation

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerFeature
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerReducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

val explorerFeaturePresentationModule = module {
    scope<ScopeQualifier.Type.Project> {
        scopedOwner {
            ExplorerReducer(
                expandDirectory = get(),
                collapseDirectory = get(),
                observeExplorerTree = get(),
                saveExplorerPosition = get(),
                openFile = get()
            )
        }

        scopedOwner {
            val projectPath = get<String>(qualifier = ScopeQualifier.Project)

            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            ExplorerFeature(path = projectPath, scope = scope, reducer = get())
        }
    }
}