package io.github.numq.haskcore.core.feature

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface Feature<out State, in Command, out Event> : AutoCloseable {
    val state: StateFlow<State>

    val events: Flow<Event>

    suspend fun execute(command: Command)
}