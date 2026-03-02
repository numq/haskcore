package io.github.numq.haskcore.service.clipboard

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import org.koin.dsl.bind
import org.koin.dsl.module
import java.awt.Toolkit

val clipboardModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner {
            val systemClipboard = Toolkit.getDefaultToolkit().systemClipboard

            DefaultClipboardService(systemClipboard = systemClipboard)
        } bind ClipboardService::class
    }
}