package io.github.numq.haskcore.feature.status.presentation

import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.status.presentation.feature.StatusFeature
import io.github.numq.haskcore.feature.status.presentation.feature.StatusReducer
import org.koin.dsl.module

val statusPresentationModule = module {
    scope<ScopeQualifierType.Project> {
        scopedOwner { StatusReducer(observeStatus = get(), updatePaths = get()) }

        scopedOwner { StatusFeature(reducer = get()) }
    }
}