package io.github.numq.haskcore.feature.welcome.presentation

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import org.koin.dsl.module

val welcomePresentationModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner { WelcomeReducer(observeRecentProjects = get(), removeRecentProject = get()) }

        scopedOwner { (title: String) ->
            WelcomeFeature(title = title, reducer = get())
        }
    }
}