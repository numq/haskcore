package io.github.numq.haskcore.application

import io.github.numq.haskcore.application.presentation.ApplicationFeature
import io.github.numq.haskcore.application.presentation.ApplicationState
import io.github.numq.haskcore.application.presentation.reducer.ApplicationReducer
import io.github.numq.haskcore.feature.factory.CommandStrategy
import io.github.numq.haskcore.feature.factory.FeatureFactory
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.dsl.module
import org.koin.dsl.onClose

@OptIn(DelicateCoroutinesApi::class)
internal val applicationModule = module {
    single {
        ApplicationFeature(
            feature = FeatureFactory().create(
                initialState = ApplicationState.Splash,
                reducer = ApplicationReducer(getSession = get(), openWorkspace = get()),
                strategy = CommandStrategy.Immediate,
            )
        )
    } onClose { GlobalScope.launch { it?.close() } }
}