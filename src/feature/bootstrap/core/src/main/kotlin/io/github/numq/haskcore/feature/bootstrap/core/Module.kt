package io.github.numq.haskcore.feature.bootstrap.core

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.bootstrap.core.usecase.Boot
import org.koin.dsl.bind
import org.koin.dsl.module

val bootstrapCoreModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner { DefaultBootstrapService() } bind BootstrapService::class

        scopedOwner {
            Boot(textServiceInitializer = get(), bootstrapService = get(), sessionService = get())
        }
    }
}