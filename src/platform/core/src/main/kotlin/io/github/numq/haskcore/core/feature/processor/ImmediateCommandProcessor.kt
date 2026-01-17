package io.github.numq.haskcore.core.feature.processor

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ImmediateCommandProcessor<Command>(
    debounceMillis: Long?, timeoutMillis: Long?
) : BaseCommandProcessor<Command>(
    debounceMillis = debounceMillis, timeoutMillis = timeoutMillis
) {
    private val mutex = Mutex()

    override suspend fun process(action: CommandProcessorAction<Command>) = ifOpen {
        checkDebounce {
            mutex.withLock {
                startOperation()

                try {
                    checkTimeout {
                        with(action) { block(command) }
                    }
                } catch (throwable: Throwable) {
                    onFailure?.invoke(throwable)
                } finally {
                    completeOperation()
                }
            }
        }
    }
}