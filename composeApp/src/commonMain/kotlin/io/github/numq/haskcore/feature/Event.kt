package io.github.numq.haskcore.feature

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
interface Event {
    val timestamp: Instant

    val payload: Any?

    companion object {
        fun createTimestamp() = Clock.System.now()
    }

    abstract class Collectable<out T>(override val payload: Any? = null) : Event {
        abstract val key: Any

        abstract val flow: Flow<T>

        override val timestamp = createTimestamp()
    }

    data class Timeout(val exception: TimeoutCancellationException, override val payload: Any? = null) : Event {
        override val timestamp = createTimestamp()
    }

    data class Cancellation(val exception: CancellationException, override val payload: Any? = null) : Event {
        override val timestamp = createTimestamp()
    }

    data class Failure(val throwable: Throwable, override val payload: Any? = null) : Event {
        override val timestamp = createTimestamp()
    }
}