package io.github.numq.haskcore.feature

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface Feature<Command, State> {
    val state: StateFlow<State>

    val events: Flow<Event>

    val invokeOnClose: (suspend () -> Unit)?

    suspend fun <T> collect(
        event: Event.Collectable<T>, joinCancellation: Boolean, action: suspend (T) -> Unit = {}
    )

    suspend fun <T> stopCollecting(key: T, joinCancellation: Boolean)

    suspend fun stopCollectingAll(joinCancellation: Boolean)

    suspend fun execute(command: Command)

    suspend fun cancel()

    suspend fun cancelAndJoin()

    suspend fun close()
}