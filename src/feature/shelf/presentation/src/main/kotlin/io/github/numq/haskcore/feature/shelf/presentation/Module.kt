package io.github.numq.haskcore.feature.shelf.presentation

import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.shelf.presentation.feature.ShelfFeature
import io.github.numq.haskcore.feature.shelf.presentation.feature.ShelfReducer
import org.koin.dsl.module

val shelfPresentationModule = module {
    scope<ScopeQualifierType.Project> {
        scopedOwner {
            ShelfReducer(
                observeShelf = get(), saveLeftRatio = get(), saveRightRatio = get(), selectShelfTool = get()
            )
        }

        scopedOwner { ShelfFeature(reducer = get()) }
    }
}