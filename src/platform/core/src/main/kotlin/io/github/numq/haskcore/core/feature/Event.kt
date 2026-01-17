package io.github.numq.haskcore.core.feature

import io.github.numq.haskcore.core.timestamp.Timestamp
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.cancellation.CancellationException

interface Event {
    val payload: Any?

    val timestamp: Timestamp

    abstract class Collectable<out T>(override val payload: Any? = null) : Event {
        abstract val key: Any

        abstract val flow: Flow<T>

        override val timestamp = Timestamp.now()
    }

    data class Timeout(val exception: TimeoutCancellationException, override val payload: Any? = null) : Event {
        override val timestamp = Timestamp.now()
    }

    data class Cancellation(val exception: CancellationException, override val payload: Any? = null) : Event {
        override val timestamp = Timestamp.now()
    }

    data class Failure(val throwable: Throwable, override val payload: Any? = null) : Event {
        override val timestamp = Timestamp.now()
    }
}