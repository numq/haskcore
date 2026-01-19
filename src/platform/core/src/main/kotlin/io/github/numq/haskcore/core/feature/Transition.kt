package io.github.numq.haskcore.core.feature

data class Transition<out State, out Effect>(val state: State, val effects: List<Effect>)

inline fun <reified State, reified Effect> State.transition() =
    Transition<State, Effect>(state = this, effects = emptyList())

inline fun <reified State, reified Effect> Transition<State, Effect>.effect(effect: Effect) =
    copy(effects = listOf(effect))

inline fun <reified State, reified Effect> Transition<State, Effect>.effects(vararg effects: Effect) =
    copy(effects = effects.toList())