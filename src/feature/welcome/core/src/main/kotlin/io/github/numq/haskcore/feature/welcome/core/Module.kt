package io.github.numq.haskcore.feature.welcome.core

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.welcome.core.usecase.ObserveRecentProjects
import io.github.numq.haskcore.feature.welcome.core.usecase.RemoveRecentProject
import org.koin.dsl.module

val welcomeCoreModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner { ObserveRecentProjects(sessionService = get()) }

        scopedOwner { RemoveRecentProject(sessionService = get()) }
    }
}