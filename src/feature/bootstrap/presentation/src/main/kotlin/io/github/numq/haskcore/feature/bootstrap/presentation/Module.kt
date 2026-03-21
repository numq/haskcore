package io.github.numq.haskcore.feature.bootstrap.presentation

import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.bootstrap.presentation.feature.BootstrapFeature
import io.github.numq.haskcore.feature.bootstrap.presentation.feature.BootstrapReducer
import org.koin.dsl.module

val bootstrapPresentationModule = module {
    scope<ScopeQualifierType.Application> {
        scopedOwner {
            BootstrapReducer(boot = get())
        }

        scopedOwner {
            BootstrapFeature(reducer = get())
        }
    }
}