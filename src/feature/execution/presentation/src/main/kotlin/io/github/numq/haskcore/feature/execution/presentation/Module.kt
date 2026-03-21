package io.github.numq.haskcore.feature.execution.presentation

import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.execution.presentation.feature.ExecutionFeature
import io.github.numq.haskcore.feature.execution.presentation.feature.ExecutionReducer
import org.koin.dsl.module

val executionPresentationModule = module {
    scope<ScopeQualifierType.Project> {
        scopedOwner { ExecutionReducer(observeExecution = get(), startExecution = get(), selectArtifact = get()) }

        scopedOwner { ExecutionFeature(reducer = get()) }
    }
}