package io.github.numq.haskcore.feature.settings.core

import io.github.numq.haskcore.core.di.ScopeQualifier
import org.koin.dsl.module

val settingsCoreModule = module {
    scope<ScopeQualifier.Application> {

    }
}