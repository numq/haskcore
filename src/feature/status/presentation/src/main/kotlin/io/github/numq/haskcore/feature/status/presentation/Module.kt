package io.github.numq.haskcore.feature.status.presentation

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import org.koin.dsl.module

val statusPresentationModule = module {
    scope<ScopeQualifier.Project> {
        scopedOwner { StatusReducer(observeStatus = get(), updatePaths = get()) }

        scopedOwner { StatusFeature(reducer = get()) }
    }
}