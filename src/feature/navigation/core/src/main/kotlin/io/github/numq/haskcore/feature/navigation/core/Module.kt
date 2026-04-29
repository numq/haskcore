package io.github.numq.haskcore.feature.navigation.core

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.feature.navigation.core.usecase.GetDestinations
import io.github.numq.haskcore.feature.navigation.core.usecase.OpenProject
import org.koin.dsl.module

val navigationFeatureCoreModule = module {
    scope<ScopeQualifier.Type.Application> {
        scopedOwner { GetDestinations(sessionApi = get()) }

        scopedOwner { OpenProject(sessionApi = get()) }
    }
}