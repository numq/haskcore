package io.github.numq.haskcore.feature.navigation.presentation

import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.navigation.core.Destination
import io.github.numq.haskcore.feature.navigation.presentation.feature.NavigationFeature
import io.github.numq.haskcore.feature.navigation.presentation.feature.NavigationReducer
import org.koin.dsl.module

val navigationPresentationModule = module {
    scope<ScopeQualifierType.Application> {
        scopedOwner { NavigationReducer(getDestinations = get(), openProject = get()) }

        scopedOwner { (initialDestinations: List<Destination>) ->
            NavigationFeature(initialDestinations = initialDestinations, reducer = get())
        }
    }
}