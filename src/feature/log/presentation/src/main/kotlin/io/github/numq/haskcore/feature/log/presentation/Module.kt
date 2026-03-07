package io.github.numq.haskcore.feature.log.presentation

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import org.koin.dsl.module

val logPresentationModule = module {
    scope<ScopeQualifier.Project> {
        scopedOwner { LogReducer(observeLogs = get()) }

        scopedOwner { LogFeature(reducer = get()) }
    }
}