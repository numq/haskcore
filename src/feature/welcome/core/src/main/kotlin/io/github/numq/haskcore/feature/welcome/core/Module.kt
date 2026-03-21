package io.github.numq.haskcore.feature.welcome.core

import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.welcome.core.usecase.ObserveRecentProjects
import io.github.numq.haskcore.feature.welcome.core.usecase.RemoveRecentProject
import org.koin.dsl.module

val welcomeCoreModule = module {
    scope<ScopeQualifierType.Application> {
        scopedOwner { ObserveRecentProjects(sessionService = get()) }

        scopedOwner { RemoveRecentProject(sessionService = get()) }
    }
}