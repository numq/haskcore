package io.github.numq.haskcore.feature.bootstrap.presentation

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.feature.bootstrap.presentation.feature.BootstrapFeature
import io.github.numq.haskcore.feature.bootstrap.presentation.feature.BootstrapReducer
import org.koin.dsl.module

val bootstrapFeaturePresentationModule = module {
    scope<ScopeQualifier.Type.Application> {
        scopedOwner {
            BootstrapReducer(boot = get())
        }

        scopedOwner {
            BootstrapFeature(reducer = get())
        }
    }
}