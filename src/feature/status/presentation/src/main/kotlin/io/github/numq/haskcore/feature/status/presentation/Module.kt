package io.github.numq.haskcore.feature.status.presentation

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.feature.status.presentation.feature.StatusFeature
import io.github.numq.haskcore.feature.status.presentation.feature.StatusReducer
import org.koin.dsl.module

val statusFeaturePresentationModule = module {
    scope<ScopeQualifier.Type.Project> {
        scopedOwner { StatusReducer(observeStatus = get(), updatePaths = get()) }

        scopedOwner { StatusFeature(reducer = get()) }
    }
}