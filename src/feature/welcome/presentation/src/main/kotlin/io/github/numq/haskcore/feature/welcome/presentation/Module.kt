package io.github.numq.haskcore.feature.welcome.presentation

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.feature.welcome.presentation.feature.WelcomeFeature
import io.github.numq.haskcore.feature.welcome.presentation.feature.WelcomeReducer
import org.koin.dsl.module

val welcomeFeaturePresentationModule = module {
    scope<ScopeQualifier.Type.Application> {
        scopedOwner { WelcomeReducer(observeRecentProjects = get(), removeRecentProject = get()) }

        scopedOwner { (title: String) ->
            WelcomeFeature(title = title, reducer = get())
        }
    }
}