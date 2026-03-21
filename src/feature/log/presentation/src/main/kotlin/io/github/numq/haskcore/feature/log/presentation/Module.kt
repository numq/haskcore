package io.github.numq.haskcore.feature.log.presentation

import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.log.presentation.feature.LogFeature
import io.github.numq.haskcore.feature.log.presentation.feature.LogReducer
import org.koin.dsl.module

val logPresentationModule = module {
    scope<ScopeQualifierType.Project> {
        scopedOwner { LogReducer(observeLogs = get()) }

        scopedOwner { LogFeature(reducer = get()) }
    }
}