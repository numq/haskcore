package io.github.numq.haskcore.core.feature

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal interface Feature<out State, in Command, out Effect> : AutoCloseable {
    val state: StateFlow<State>

    val effects: Flow<Effect>

    suspend fun execute(command: Command)
}