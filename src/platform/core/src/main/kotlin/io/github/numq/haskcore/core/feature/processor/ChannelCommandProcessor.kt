package io.github.numq.haskcore.core.feature.processor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

internal class ChannelCommandProcessor<Command>(
    capacity: Int, coroutineContext: CoroutineContext, debounceMillis: Long?, timeoutMillis: Long?
) : BaseCommandProcessor<Command>(debounceMillis = debounceMillis, timeoutMillis = timeoutMillis) {
    private val mutex = Mutex()

    private val coroutineScope = CoroutineScope(coroutineContext)

    private val actionChannel = Channel<CommandProcessorAction<Command>>(capacity)

    init {
        coroutineScope.launch(coroutineContext) {
            for ((command, block) in actionChannel) {
                mutex.withLock {
                    startOperation()

                    try {
                        checkTimeout {
                            block(command)
                        }
                    } catch (throwable: Throwable) {
                        if (throwable !is CancellationException) {
                            onFailure?.invoke(throwable)
                        }
                    } finally {
                        completeOperation()
                    }
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun ifOpen(block: suspend () -> Unit) {
        super.ifOpen(block)

        if (actionChannel.isClosedForSend) {
            throw CancellationException("Processor closed")
        }

        block()
    }

    override suspend fun process(action: CommandProcessorAction<Command>) = ifOpen {
        checkDebounce {
            checkTimeout {
                actionChannel.send(action)
            }
        }
    }

    override fun close() {
        coroutineScope.cancel()

        actionChannel.close()

        super.close()
    }
}