package io.github.numq.haskcore.service.keymap

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import org.koin.dsl.bind
import org.koin.dsl.module

val keymapModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner {
            DefaultKeymapService(keymapData = mapOf(KeymapContext.EDITOR to Keymap.editor))
        } bind KeymapService::class
    }
}