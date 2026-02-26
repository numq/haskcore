package io.github.numq.haskcore.service.runtime

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import org.koin.dsl.bind
import org.koin.dsl.module

val runtimeModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner { LocalRuntimeService() } bind RuntimeService::class
    }
}