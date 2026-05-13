package io.github.numq.haskcore.feature.execution.presentation

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.feature.execution.presentation.feature.ExecutionFeature
import io.github.numq.haskcore.feature.execution.presentation.feature.ExecutionReducer
import org.koin.dsl.module

val executionFeaturePresentationModule = module {
    scope<ScopeQualifier.Type.Project> {
        scopedOwner {
            ExecutionReducer(
                buildConfiguration = get(),
                deleteConfiguration = get(),
                editConfiguration = get(),
                observeExecution = get(),
                runConfiguration = get(),
                setCurrentConfiguration = get(),
                stopConfiguration = get()
            )
        }

        scopedOwner { ExecutionFeature(reducer = get()) }
    }
}