package io.github.numq.haskcore.feature.settings.core

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import org.koin.dsl.module

val settingsFeatureCoreModule = module {
    scope<ScopeQualifier.Type.Application> {

    }
}