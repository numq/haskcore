package io.github.numq.haskcore.feature.explorer.presentation

import io.github.numq.haskcore.core.di.ScopePath
import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import org.koin.dsl.module

val explorerPresentationModule = module {
    scope<ScopeQualifier.Project> {
        scopedOwner {
            ExplorerReducer(
                observeExplorerTree = get(),
                saveExplorerPosition = get(),
                selectExplorerNode = get(),
                toggleExplorerNode = get(),
                openFile = get()
            )
        }

        scopedOwner {
            val projectPath = get<String>(qualifier = ScopePath.Project)

            ExplorerFeature(path = projectPath, reducer = get())
        }
    }
}