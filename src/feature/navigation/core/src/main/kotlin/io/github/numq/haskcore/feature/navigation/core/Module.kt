package io.github.numq.haskcore.feature.navigation.core

import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.navigation.core.usecase.GetDestinations
import io.github.numq.haskcore.feature.navigation.core.usecase.OpenProject
import org.koin.dsl.bind
import org.koin.dsl.module

val navigationCoreModule = module {
    scope<ScopeQualifierType.Application> {
        scopedOwner { DefaultNavigationService() } bind NavigationService::class

        scopedOwner { GetDestinations(navigationService = get(), sessionService = get()) }

        scopedOwner { OpenProject(sessionService = get()) }
    }
}