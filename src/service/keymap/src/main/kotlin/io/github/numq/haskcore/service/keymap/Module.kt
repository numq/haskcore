package io.github.numq.haskcore.service.keymap

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import org.koin.dsl.bind
import org.koin.dsl.module

val keymapServiceModule = module {
    scope<ScopeQualifier.Type.Application> {
        scopedOwner {
            DefaultKeymapService(actionsByContext = Keymap.actionsByContext)
        } bind KeymapService::class
    }
}