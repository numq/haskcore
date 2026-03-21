package io.github.numq.haskcore.feature.welcome.presentation

import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.welcome.presentation.feature.WelcomeFeature
import io.github.numq.haskcore.feature.welcome.presentation.feature.WelcomeReducer
import org.koin.dsl.module

val welcomePresentationModule = module {
    scope<ScopeQualifierType.Application> {
        scopedOwner { WelcomeReducer(observeRecentProjects = get(), removeRecentProject = get()) }

        scopedOwner { (title: String) ->
            WelcomeFeature(title = title, reducer = get())
        }
    }
}