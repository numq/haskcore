package io.github.numq.haskcore.platform.core.feature

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.atomicfu.update
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.*
import kotlin.coroutines.cancellation.CancellationException

internal class ReducerFeature<State, in Command, out Event>(
    initialState: State,
    initialCommands: List<Command> = emptyList(),
    private val scope: CoroutineScope,
    private val reducer: Reducer<State, Command, Event>,
) : Feature<State, Command, Event> {
    private val isClosed = atomic(false)

    private val jobs = atomic(mapOf<Any, Job>())

    private val _commands = Channel<Command>(Channel.UNLIMITED)

    private val _events = MutableSharedFlow<Event>(0, Int.MAX_VALUE)

    override val events = _events.asSharedFlow()

    override val state = _commands.receiveAsFlow().scan(initialState) { state, command ->
        val transition = reducer.reduce(state, command)

        transition.effects.forEach(::processEffect)

        transition.events.forEach(_events::tryEmit)

        transition.state
    }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = initialState)

    init {
        initialCommands.forEach { command ->
            scope.launch { execute(command) }
        }
    }

    private fun processEffect(effect: Effect) {
        when (effect) {
            is Effect.Stream<*> -> launchManaged(effect.key) {
                try {
                    when (effect.strategy) {
                        Effect.Stream.Strategy.Sequential -> effect.flow.collect { cmd ->
                            @Suppress("UNCHECKED_CAST") execute(cmd as Command)
                        }

                        Effect.Stream.Strategy.Restart -> effect.flow.collectLatest { cmd ->
                            @Suppress("UNCHECKED_CAST") execute(cmd as Command)
                        }
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (throwable: Throwable) {
                    val cmd = effect.fallback?.invoke(throwable)

                    if (cmd != null) {
                        @Suppress("UNCHECKED_CAST") execute(cmd as Command)
                    }
                }
            }

            is Effect.Action<*> -> launchManaged(effect.key) {
                val cmd = try {
                    effect.block()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (throwable: Throwable) {
                    effect.fallback?.invoke(throwable)
                }

                if (cmd != null) {
                    @Suppress("UNCHECKED_CAST") execute(cmd as Command)
                }
            }

            is Effect.Cancel -> cancelJob(effect.key)
        }
    }

    private fun launchManaged(key: Any, block: suspend () -> Unit) {
        val newJob = scope.launch { block() }

        val oldJob = jobs.getAndUpdate { current ->
            current + Pair(key, newJob)
        }[key]

        oldJob?.cancel()

        newJob.invokeOnCompletion {
            jobs.update { current ->
                when {
                    current[key] === newJob -> current - key

                    else -> current
                }
            }
        }
    }

    private fun cancelJob(key: Any) {
        jobs.update { current ->
            current[key]?.cancel()

            current - key
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun execute(command: Command) {
        if (isClosed.value) return

        try {
            _commands.send(command)
        } catch (_: ClosedSendChannelException) {
        }
    }

    override fun close() {
        if (!isClosed.compareAndSet(expect = false, update = true)) return

        scope.cancel()

        _commands.close()

        jobs.update { current ->
            current.values.forEach(Job::cancel)

            emptyMap()
        }
    }
}