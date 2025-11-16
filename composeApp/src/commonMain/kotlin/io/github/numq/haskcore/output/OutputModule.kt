package io.github.numq.haskcore.output

import io.github.numq.haskcore.feature.factory.CommandStrategy
import io.github.numq.haskcore.feature.factory.FeatureFactory
import io.github.numq.haskcore.output.presentation.OutputFeature
import io.github.numq.haskcore.output.presentation.OutputReducer
import io.github.numq.haskcore.output.presentation.OutputState
import io.github.numq.haskcore.output.usecase.CloseOutput
import io.github.numq.haskcore.output.usecase.ObserveOutputs
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose

@OptIn(DelicateCoroutinesApi::class)
internal val outputModule = module {
    single { OutputRepository.Default() } bind OutputRepository::class

    single { ObserveOutputs(outputRepository = get()) }

    single { CloseOutput(outputRepository = get()) }

    single {
        OutputFeature(
            feature = FeatureFactory().create(
                initialState = OutputState(), reducer = OutputReducer(
                    closeOutput = get(),
                    observeOutputs = get(),
                    observeWorkspace = get()
                ), strategy = CommandStrategy.Immediate
            )
        )
    } onClose { GlobalScope.launch { it?.close() } }
}