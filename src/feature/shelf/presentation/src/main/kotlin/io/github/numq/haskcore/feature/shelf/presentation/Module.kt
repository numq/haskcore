package io.github.numq.haskcore.feature.shelf.presentation

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import org.koin.dsl.module

val shelfPresentationModule = module {
    scope<ScopeQualifier.Project> {
        scopedOwner {
            ShelfReducer(
                observeShelf = get(), saveLeftRatio = get(), saveRightRatio = get(), selectShelfTool = get()
            )
        }

        scopedOwner { ShelfFeature(reducer = get()) }
    }
}