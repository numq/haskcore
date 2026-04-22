package io.github.numq.haskcore.feature.log.core

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.feature.log.core.usecase.ObserveLogs
import org.koin.dsl.module

val logFeatureCoreModule = module {
    scope<ScopeQualifier.Type.Project> {
        scopedOwner { ObserveLogs(loggerApi = get()) }
    }
}