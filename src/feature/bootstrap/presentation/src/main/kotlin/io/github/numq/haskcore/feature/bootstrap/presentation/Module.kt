package io.github.numq.haskcore.feature.bootstrap.presentation

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import org.koin.dsl.module

val bootstrapPresentationModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner {
            BootstrapReducer(boot = get())
        }

        scopedOwner {
            BootstrapFeature(reducer = get())
        }
    }
}