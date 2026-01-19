package io.github.numq.haskcore.core.feature

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

abstract class BaseFeature<State, in Command, out FeatureEffect : Effect>(
    initialState: State, private val scope: CoroutineScope, private val reducer: Reducer<State, Command, FeatureEffect>
) : Feature<State, Command, FeatureEffect> {
    private val isClosed = AtomicBoolean(false)

    private val jobs = ConcurrentHashMap<Any, Job>()

    private val _commands = Channel<Command>(Channel.UNLIMITED)

    private val _effects = MutableSharedFlow<FeatureEffect>(0, Int.MAX_VALUE)

    override val effects = _effects.asSharedFlow()

    override val state = _commands.receiveAsFlow().scan(initialState) { state, command ->
        val transition = reducer.reduce(state, command)

        transition.effects.forEach { effect ->
            processEffect(effect)
        }

        transition.state
    }.stateIn(scope, SharingStarted.Eagerly, initialState)

    private fun processEffect(effect: FeatureEffect) {
        when (effect) {
            is Effect.Notify<*> -> _effects.tryEmit(effect)

            is Effect.Collect<*> -> launchManaged(effect.key) {
                try {
                    when (effect.strategy) {
                        Effect.Collect.Strategy.Sequential -> effect.flow.collect { cmd ->
                            @Suppress("UNCHECKED_CAST") execute(cmd as Command)
                        }

                        Effect.Collect.Strategy.Restart -> effect.flow.collectLatest { cmd ->
                            @Suppress("UNCHECKED_CAST") execute(cmd as Command)
                        }
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (throwable: Throwable) {
                    val cmd = effect.fallback(throwable)

                    @Suppress("UNCHECKED_CAST") execute(cmd as Command)
                }
            }

            is Effect.Execute<*> -> launchManaged(effect.key) {
                val cmd = try {
                    effect.block()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (throwable: Throwable) {
                    effect.fallback(throwable)
                }

                @Suppress("UNCHECKED_CAST") execute(cmd as Command)
            }

            is Effect.Cancel -> cancelJob(effect.key)
        }
    }

    private fun launchManaged(key: Any, block: suspend () -> Unit) {
        jobs[key]?.cancel()

        val job = scope.launch { block() }

        job.invokeOnCompletion { jobs.remove(key, job) }

        jobs[key] = job
    }

    private fun cancelJob(key: Any) {
        jobs.remove(key)?.cancel()
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun execute(command: Command) {
        if (isClosed.get()) return

        try {
            _commands.send(command)
        } catch (_: ClosedSendChannelException) {
        }
    }

    override fun close() {
        if (!isClosed.compareAndSet(false, true)) return

        _commands.close()

        jobs.values.forEach(Job::cancel)

        jobs.clear()
    }
}