package io.github.numq.haskcore.core.feature.processor

import io.github.numq.haskcore.core.timestamp.Timestamp
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.concurrent.Volatile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal abstract class BaseCommandProcessor<Command>(
    private val debounceMillis: Long?,
    private val timeoutMillis: Long?,
) : CommandProcessor<Command> {
    private val debounceMutex = Mutex()

    private val activeOperationsRef = atomic(0)

    private var previousTime = Duration.INFINITE

    @Volatile
    private var isClosed = false

    internal fun startOperation() {
        activeOperationsRef.incrementAndGet()
    }

    internal fun completeOperation() {
        activeOperationsRef.decrementAndGet()
    }

    internal open suspend fun ifOpen(block: suspend () -> Unit) {
        check(!isClosed) { "Processor closed" }

        block()
    }

    internal suspend fun checkDebounce(block: suspend () -> Unit) {
        val debounceTime = debounceMillis?.milliseconds ?: return block()

        debounceMutex.withLock {
            val currentTime = Timestamp.now().milliseconds

            when {
                previousTime.isInfinite() -> {
                    block()

                    previousTime = currentTime
                }

                currentTime - previousTime >= debounceTime -> {
                    block()

                    previousTime = currentTime
                }
            }
        }
    }

    internal suspend fun checkTimeout(block: suspend () -> Unit) {
        val timeoutTime = timeoutMillis ?: return block()

        withTimeout(timeMillis = timeoutTime) {
            block()
        }
    }

    override val activeOperations: Int get() = activeOperationsRef.value

    override var onFailure: (suspend (Throwable) -> Unit)? = null
        set(value) {
            if (!isClosed) {
                field = value
            }
        }

    override fun close() {
        if (!isClosed) return

        onFailure = null

        isClosed = true
    }
}