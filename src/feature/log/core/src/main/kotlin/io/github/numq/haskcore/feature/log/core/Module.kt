package io.github.numq.haskcore.feature.log.core

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.log.core.usecase.ObserveLogs
import org.koin.dsl.module

val logCoreModule = module {
    scope<ScopeQualifier.Project> {
        scopedOwner { ObserveLogs(loggerService = get()) }
    }
}