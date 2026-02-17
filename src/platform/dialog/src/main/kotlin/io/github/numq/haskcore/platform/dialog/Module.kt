package io.github.numq.haskcore.platform.dialog

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import org.koin.dsl.bind
import org.koin.dsl.module

val dialogModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner { DesktopFilePicker() } bind FilePicker::class
    }
}