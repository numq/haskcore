package io.github.numq.haskcore.core.feature

import io.github.numq.haskcore.core.feature.processor.CommandProcessor
import io.github.numq.haskcore.core.feature.processor.CommandProcessorAction
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.atomicfu.update
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

@OptIn(DelicateCoroutinesApi::class)
class BaseFeature<Command, State>(
    initialState: State,
    coroutineContext: CoroutineContext,
    private val reducer: Reducer<Command, State>,
    private val commandProcessor: CommandProcessor<Command>,
) : Feature<Command, State> {
    private val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())

    private val _state = MutableStateFlow(initialState)

    override val state = _state.asStateFlow()

    private val _events = Channel<Event>(Channel.UNLIMITED)

    override val events = _events.receiveAsFlow()

    private val jobsMutex = Mutex()

    private val jobs = mutableMapOf<Any, Job>()

    private val currentTransition = atomic<Deferred<Transition<State>>?>(null)

    @Volatile
    private var isClosed = false

    private fun requireOpen() {
        if (isClosed || _events.isClosedForSend) {
            error("Feature closed")
        }
    }

    private suspend fun perform(command: Command) {
        val transition = coroutineScope.async {
            reducer.reduce(_state.value, command)
        }

        currentTransition.update { transition }

        try {
            val (state, events) = transition.await()

            _state.emit(state)

            events.forEach { event ->
                if (!_events.isClosedForSend) {
                    _events.send(event)
                }
            }
        } finally {
            currentTransition.update { null }
        }
    }

    internal suspend fun dispatchFailure(throwable: Throwable) {
        if (!isClosed && !_events.isClosedForSend) {
            val event = when (throwable) {
                is TimeoutCancellationException -> Event.Timeout(exception = throwable)

                is CancellationException -> Event.Cancellation(exception = throwable)

                else -> Event.Failure(throwable = throwable)
            }

            if (!_events.isClosedForSend) {
                _events.send(event)
            }
        }
    }

    override var invokeOnClose: (suspend () -> Unit)? = null
        private set

    override suspend fun <T> collect(
        event: Event.Collectable<T>, joinCancellation: Boolean, action: suspend (T) -> Unit,
    ) {
        jobsMutex.withLock {
            try {
                requireOpen()

                val key = event.key.toString()

                jobs[key]?.run {
                    job.cancel()

                    if (joinCancellation) {
                        job.join()
                    }
                }

                jobs[key] = event.flow.onEach(action).launchIn(coroutineScope)
            } catch (throwable: Throwable) {
                if (!_events.isClosedForSend) {
                    _events.send(Event.Failure(throwable = throwable))
                }
            }
        }
    }

    override suspend fun <T> stopCollecting(key: T, joinCancellation: Boolean) {
        jobsMutex.withLock {
            try {
                requireOpen()

                val job = jobs.remove("$key") ?: return

                job.cancel()

                if (joinCancellation) {
                    job.join()
                }
            } catch (throwable: Throwable) {
                if (!_events.isClosedForSend) {
                    _events.send(Event.Failure(throwable = throwable))
                }
            }
        }
    }

    override suspend fun stopCollectingAll(joinCancellation: Boolean) {
        jobsMutex.withLock {
            try {
                requireOpen()

                val allJobs = jobs.values.toList()

                jobs.clear()

                allJobs.forEach { job ->
                    job.cancel()

                    if (joinCancellation) {
                        job.join()
                    }
                }
            } catch (throwable: Throwable) {
                if (!_events.isClosedForSend) {
                    _events.send(Event.Failure(throwable = throwable))
                }
            }
        }
    }

    override suspend fun execute(command: Command) {
        try {
            requireOpen()

            commandProcessor.process(CommandProcessorAction(command = command, block = ::perform))
        } catch (throwable: Throwable) {
            if (!_events.isClosedForSend) {
                _events.send(Event.Failure(throwable = throwable))
            }
        }
    }

    override suspend fun cancel() {
        try {
            requireOpen()

            currentTransition.getAndUpdate { null }?.cancel()

            if (!_events.isClosedForSend) {
                _events.send(Event.Cancellation(exception = CancellationException("Transition cancelled")))
            }
        } catch (throwable: Throwable) {
            if (!_events.isClosedForSend) {
                _events.send(Event.Failure(throwable = throwable))
            }
        }
    }

    override suspend fun cancelAndJoin() {
        try {
            requireOpen()

            currentTransition.getAndUpdate { null }?.cancelAndJoin()

            if (!_events.isClosedForSend) {
                _events.send(Event.Cancellation(exception = CancellationException("Transition cancelled")))
            }
        } catch (throwable: Throwable) {
            if (!_events.isClosedForSend) {
                _events.send(Event.Failure(throwable = throwable))
            }
        }
    }

    override suspend fun close() {
        if (isClosed) return

        try {
            coroutineScope.cancel()

            jobsMutex.withLock {
                jobs.clear()
            }

            _events.close()

            commandProcessor.close()

            invokeOnClose?.invoke()
        } finally {
            isClosed = true
        }
    }
}