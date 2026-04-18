package io.github.numq.haskcore.feature.bootstrap.core

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.feature.bootstrap.core.usecase.Boot
import org.koin.dsl.bind
import org.koin.dsl.module

val bootstrapFeatureCoreModule = module {
    scope<ScopeQualifier.Type.Application> {
        scopedOwner { DefaultBootstrapService() } bind BootstrapService::class

        scopedOwner { Boot(sessionApi = get(), bootstrapService = get(), highlightingServiceInitializer = get()) }
    }
}