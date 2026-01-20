package io.github.numq.haskcore.core.feature

import kotlinx.coroutines.flow.Flow

fun interface Reducer<State, in Command, out Event> {
    fun reduce(state: State, command: Command): Transition<State, Event>

    fun transition(state: State) = Transition<State, Event>(state = state, events = emptyList(), effects = emptyList())
}

fun <State, Command, Event> Reducer<State, Command, Event>.stream(
    key: Any,
    flow: Flow<Command>,
    strategy: Effect.Stream.Strategy = Effect.Stream.Strategy.Sequential,
    fallback: (suspend (Throwable) -> Command)? = null
) = Effect.Stream(key, flow, strategy, fallback)

fun <State, Command, Event> Reducer<State, Command, Event>.action(
    key: Any, fallback: (suspend (Throwable) -> Command)? = null, block: suspend () -> Command
) = Effect.Action(key, fallback, block)

fun <State, Command, Event> Reducer<State, Command, Event>.cancel(key: Any) = Effect.Cancel(key)

fun <State, Command, Event> Reducer<State, Command, Event>.combine(
    other: Reducer<State, Command, Event>
) = Reducer<State, Command, Event> { state, command ->
    val src = this.reduce(state = state, command = command)

    val dst = other.reduce(state = src.state, command = command)

    Transition(state = dst.state, events = src.events + dst.events, effects = src.effects + dst.effects)
}