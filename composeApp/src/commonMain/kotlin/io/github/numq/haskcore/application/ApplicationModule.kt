package io.github.numq.haskcore.application

import io.github.numq.haskcore.application.presentation.ApplicationFeature
import io.github.numq.haskcore.application.presentation.ApplicationState
import io.github.numq.haskcore.application.presentation.reducer.ApplicationReducer
import io.github.numq.haskcore.feature.factory.CommandStrategy
import io.github.numq.haskcore.feature.factory.FeatureFactory
import org.koin.dsl.module

internal val applicationModule = module {
    single {
        ApplicationFeature(
            feature = FeatureFactory().create(
                initialState = ApplicationState.Splash,
                reducer = ApplicationReducer(getSession = get(), openWorkspace = get()),
                strategy = CommandStrategy.Immediate,
            )
        )
    }
}