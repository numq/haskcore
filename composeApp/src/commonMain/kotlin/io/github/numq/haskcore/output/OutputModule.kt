package io.github.numq.haskcore.output

import io.github.numq.haskcore.feature.factory.CommandStrategy
import io.github.numq.haskcore.feature.factory.FeatureFactory
import io.github.numq.haskcore.output.presentation.OutputFeature
import io.github.numq.haskcore.output.presentation.OutputReducer
import io.github.numq.haskcore.output.presentation.OutputState
import io.github.numq.haskcore.output.usecase.ClearOutput
import io.github.numq.haskcore.output.usecase.ObserveOutput
import org.koin.dsl.bind
import org.koin.dsl.module

internal val outputModule = module {
    single { OutputRepository.Default() } bind OutputRepository::class

    single { ClearOutput(outputRepository = get()) }

    single { ObserveOutput(outputRepository = get()) }

    single { (id: String) ->
        OutputFeature(
            feature = FeatureFactory().create(
                initialState = OutputState(outputLines = listOf()),
                reducer = OutputReducer(id = id, observeOutput = get(), clearOutput = get()),
                strategy = CommandStrategy.Immediate,
            )
        )
    }
}