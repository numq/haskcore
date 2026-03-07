package io.github.numq.haskcore.feature.navigation.presentation

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.navigation.core.Destination
import org.koin.dsl.module

val navigationPresentationModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner { NavigationReducer(getDestinations = get(), openProject = get()) }

        scopedOwner { (initialDestinations: List<Destination>) ->
            NavigationFeature(initialDestinations = initialDestinations, reducer = get())
        }
    }
}