package io.github.numq.haskcore.feature.output.presentation

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.feature.output.presentation.feature.OutputFeature
import io.github.numq.haskcore.feature.output.presentation.feature.OutputReducer
import org.koin.dsl.module

val outputFeaturePresentationModule = module {
    scope<ScopeQualifier.Type.Project> {
        scopedOwner {
            OutputReducer(
                closeOutputSession = get(), copySessionText = get(), observeOutput = get(), openOutputSession = get()
            )
        }

        scopedOwner { OutputFeature(reducer = get()) }
    }
}