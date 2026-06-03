package io.github.numq.haskcore.feature.status.core

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.feature.status.core.usecase.ObserveStatus
import io.github.numq.haskcore.feature.status.core.usecase.UpdatePaths
import org.koin.dsl.bind
import org.koin.dsl.module

val statusFeatureCoreModule = module {
    scope<ScopeQualifier.Type.Project> {
        scopedOwner { DefaultStatusService() } bind StatusService::class

        scopedOwner {
            ObserveStatus(statusService = get(), projectService = get(), toolchainService = get())
        }

        scopedOwner { UpdatePaths(toolchainService = get()) }
    }
}