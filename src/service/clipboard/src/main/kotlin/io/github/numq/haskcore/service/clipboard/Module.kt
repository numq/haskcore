package io.github.numq.haskcore.service.clipboard

import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module
import java.awt.Toolkit

val clipboardModule = module {
    scope<ScopeQualifierType.Application> {
        scopedOwner {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            val systemClipboard = Toolkit.getDefaultToolkit().systemClipboard

            DefaultClipboardService(scope = scope, systemClipboard = systemClipboard)
        } bind ClipboardService::class
    }
}