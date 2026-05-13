package io.github.numq.haskcore.service.text

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.service.text.buffer.RopeTextBufferFactory
import io.github.numq.haskcore.service.text.buffer.TextBufferFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module

val textServiceModule = module {
    scope<ScopeQualifier.Type.Document> {
        scopedOwner { RopeTextBufferFactory() } bind TextBufferFactory::class

        scopedOwner {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            LocalTextService(scope = scope, bufferFactory = get())
        } bind TextService::class
    }
}