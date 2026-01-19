package io.github.numq.haskcore.core.feature

fun interface Reducer<State, in Command, out Effect> {
    fun reduce(state: State, command: Command): Transition<State, Effect>
}

fun <State, Command, Effect> Reducer<State, Command, Effect>.combine(
    other: Reducer<State, Command, Effect>
) = Reducer<State, Command, Effect> { state, command ->
    val src = this.reduce(state = state, command = command)

    val dst = other.reduce(state = src.state, command = command)

    Transition(state = dst.state, effects = src.effects + dst.effects)
}