package io.github.numq.haskcore.feature.log.presentation

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.feature.log.presentation.feature.LogFeature
import io.github.numq.haskcore.feature.log.presentation.feature.LogReducer
import org.koin.dsl.module

val logFeaturePresentationModule = module {
    scope<ScopeQualifier.Type.Project> {
        scopedOwner { LogReducer(observeLogs = get()) }

        scopedOwner { LogFeature(reducer = get()) }
    }
}