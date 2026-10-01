package io.github.numq.haskcore.feature.stack.presentation

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import io.github.numq.haskcore.feature.stack.presentation.feature.StackFeature
import io.github.numq.haskcore.feature.stack.presentation.feature.StackReducer
import org.koin.dsl.module

val stackFeaturePresentationModule = module {
    scope<ScopeQualifier.Type.Project> {
        scopedOwner { StackReducer(clearLogs = get(), observeLogs = get()) }

        scopedOwner { StackFeature(reducer = get()) }
    }
}