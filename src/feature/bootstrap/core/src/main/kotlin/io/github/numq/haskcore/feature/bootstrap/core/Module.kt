package io.github.numq.haskcore.feature.bootstrap.core

import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.bootstrap.core.usecase.Boot
import org.koin.dsl.bind
import org.koin.dsl.module

val bootstrapCoreModule = module {
    scope<ScopeQualifierType.Application> {
        scopedOwner { DefaultBootstrapService() } bind BootstrapService::class

        scopedOwner { Boot(bootstrapService = get(), sessionService = get(), highlightingServiceInitializer = get()) }
    }
}