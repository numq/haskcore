package io.github.numq.haskcore.feature.navigation.presentation

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.feature.navigation.core.Destination
import io.github.numq.haskcore.feature.navigation.presentation.feature.NavigationFeature
import io.github.numq.haskcore.feature.navigation.presentation.feature.NavigationReducer
import org.koin.dsl.module

val navigationFeaturePresentationModule = module {
    scope<ScopeQualifier.Type.Application> {
        scopedOwner { NavigationReducer(getDestinations = get(), openProject = get()) }

        scopedOwner { (initialDestinations: List<Destination>) ->
            NavigationFeature(initialDestinations = initialDestinations, reducer = get())
        }
    }
}