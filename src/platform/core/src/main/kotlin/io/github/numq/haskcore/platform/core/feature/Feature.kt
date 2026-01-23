package io.github.numq.haskcore.platform.core.feature

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalStdlibApi::class)
interface Feature<out State, in Command, out Event> : AutoCloseable {
    val state: StateFlow<State>

    val events: Flow<Event>

    suspend fun execute(command: Command)

    companion object {
        operator fun <State, Command, Event> invoke(
            initialState: State,
            scope: CoroutineScope,
            reducer: Reducer<State, Command, Event>,
            vararg initialCommands: Command
        ): Feature<State, Command, Event> = ReducerFeature(
            initialState = initialState, initialCommands = initialCommands.toList(), scope = scope, reducer = reducer
        )
    }
}