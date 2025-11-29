package io.github.numq.haskcore.feature.factory

import io.github.numq.haskcore.feature.BaseFeature
import io.github.numq.haskcore.feature.Feature
import io.github.numq.haskcore.feature.Reducer
import io.github.numq.haskcore.feature.processor.ChannelCommandProcessor
import io.github.numq.haskcore.feature.processor.ImmediateCommandProcessor
import io.github.numq.haskcore.feature.processor.ParallelCommandProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.CoroutineContext

internal class FeatureFactory {
    fun <Command, State> create(
        initialState: State,
        reducer: Reducer<Command, State>,
        strategy: CommandStrategy,
        coroutineContext: CoroutineContext = Dispatchers.Default,
        debounceMillis: Long? = null,
        timeoutMillis: Long? = null,
    ): Feature<Command, State> {
        val commandProcessor = when (strategy) {
            is CommandStrategy.Immediate -> ImmediateCommandProcessor<Command>(
                debounceMillis = debounceMillis, timeoutMillis = timeoutMillis
            )

            is CommandStrategy.Channel -> {
                val capacity = when (strategy) {
                    is CommandStrategy.Channel.Unlimited -> Channel.UNLIMITED

                    is CommandStrategy.Channel.Rendezvous -> Channel.RENDEZVOUS

                    is CommandStrategy.Channel.Conflated -> Channel.CONFLATED

                    is CommandStrategy.Channel.Fixed -> strategy.capacity
                }

                ChannelCommandProcessor(
                    capacity = capacity,
                    coroutineContext = coroutineContext,
                    debounceMillis = debounceMillis,
                    timeoutMillis = timeoutMillis
                )
            }

            is CommandStrategy.Parallel -> ParallelCommandProcessor(
                limit = strategy.limit,
                debounceMillis = debounceMillis,
                timeoutMillis = timeoutMillis,
            )
        }

        val feature = BaseFeature(
            initialState = initialState,
            coroutineContext = coroutineContext,
            reducer = reducer,
            commandProcessor = commandProcessor
        )

        commandProcessor.onFailure = feature::dispatchFailure

        return feature
    }
}