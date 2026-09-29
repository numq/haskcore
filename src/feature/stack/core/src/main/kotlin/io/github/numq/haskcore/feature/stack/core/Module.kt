package io.github.numq.haskcore.feature.stack.core

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.feature.stack.core.usecase.ClearLogs
import io.github.numq.haskcore.feature.stack.core.usecase.ObserveLogs
import org.koin.dsl.module

val stackFeatureCoreModule = module {
    scope<ScopeQualifier.Type.Project> {
        scopedOwner { ClearLogs(loggerService = get()) }

        scopedOwner { ObserveLogs(loggerService = get()) }
    }
}