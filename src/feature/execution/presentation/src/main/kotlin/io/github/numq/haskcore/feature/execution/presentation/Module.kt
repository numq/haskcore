package io.github.numq.haskcore.feature.execution.presentation

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import org.koin.dsl.module

val executionPresentationModule = module {
    scope<ScopeQualifier.Project> {
        scopedOwner { ExecutionReducer(observeExecution = get(), startExecution = get(), selectArtifact = get()) }

        scopedOwner { ExecutionFeature(reducer = get()) }
    }
}