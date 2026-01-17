package io.github.numq.haskcore.core.feature

interface Reducer<in Command, State> {
    suspend fun reduce(state: State, command: Command): Transition<State>

    fun transition(state: State, vararg event: Event) = Transition(state = state, events = event.asList())
}