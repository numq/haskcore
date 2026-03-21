package io.github.numq.haskcore.feature.log.core

import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.log.core.usecase.ObserveLogs
import org.koin.dsl.module

val logCoreModule = module {
    scope<ScopeQualifierType.Project> {
        scopedOwner { ObserveLogs(loggerService = get()) }
    }
}