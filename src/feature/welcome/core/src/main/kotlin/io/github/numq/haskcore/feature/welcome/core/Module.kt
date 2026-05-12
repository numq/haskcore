package io.github.numq.haskcore.feature.welcome.core

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.feature.welcome.core.usecase.ObserveRecentProjects
import io.github.numq.haskcore.feature.welcome.core.usecase.RemoveRecentProject
import org.koin.dsl.module

val welcomeFeatureCoreModule = module {
    scope<ScopeQualifier.Type.Application> {
        scopedOwner { ObserveRecentProjects(sessionApi = get()) }

        scopedOwner { RemoveRecentProject(sessionApi = get()) }
    }
}