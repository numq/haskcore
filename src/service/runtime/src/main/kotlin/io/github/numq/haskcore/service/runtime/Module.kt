package io.github.numq.haskcore.service.runtime

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module

val runtimeModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            LocalRuntimeService(scope = scope)
        } bind RuntimeService::class
    }
}