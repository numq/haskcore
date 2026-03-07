package io.github.numq.haskcore.feature.output.presentation

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import org.koin.dsl.module

val outputPresentationModule = module {
    scope<ScopeQualifier.Project> {
        scopedOwner {
            OutputReducer(observeOutput = get(), selectOutputSession = get(), closeOutputSession = get())
        }

        scopedOwner { OutputFeature(reducer = get()) }
    }
}