package io.github.numq.haskcore.feature.processor

import kotlinx.coroutines.sync.Semaphore

internal class ParallelCommandProcessor<Command>(
    limit: Int, debounceMillis: Long?, timeoutMillis: Long?
) : BaseCommandProcessor<Command>(
    debounceMillis = debounceMillis, timeoutMillis = timeoutMillis
) {
    private val semaphore = Semaphore(limit)

    override suspend fun process(action: CommandProcessorAction<Command>) = ifOpen {
        checkDebounce {
            semaphore.acquire()

            startOperation()

            try {
                checkTimeout {
                    with(action) {
                        block(command)
                    }
                }
            } catch (throwable: Throwable) {
                onFailure?.invoke(throwable)
            } finally {
                semaphore.release()

                completeOperation()
            }
        }
    }
}