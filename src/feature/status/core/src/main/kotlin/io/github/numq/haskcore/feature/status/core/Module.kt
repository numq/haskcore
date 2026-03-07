package io.github.numq.haskcore.feature.status.core

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.status.core.usecase.ObserveStatus
import io.github.numq.haskcore.feature.status.core.usecase.UpdatePaths
import org.koin.dsl.module

val statusCoreModule = module {
    scope<ScopeQualifier.Project> {
        scopedOwner { ObserveStatus(toolchainService = get()) }

        scopedOwner { UpdatePaths(toolchainService = get()) }
    }
}