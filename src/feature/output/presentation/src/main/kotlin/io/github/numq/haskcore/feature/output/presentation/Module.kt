package io.github.numq.haskcore.feature.output.presentation

import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.output.presentation.feature.OutputFeature
import io.github.numq.haskcore.feature.output.presentation.feature.OutputReducer
import org.koin.dsl.module

val outputPresentationModule = module {
    scope<ScopeQualifierType.Project> {
        scopedOwner {
            OutputReducer(
                closeOutputSession = get(), copySessionText = get(), observeOutput = get(), openOutputSession = get()
            )
        }

        scopedOwner { OutputFeature(reducer = get()) }
    }
}