package io.github.numq.haskcore.core.feature

data class Transition<out State, out Event>(val state: State, val events: List<Event>, val effects: List<Effect>)

fun <State, Event> Transition<State, Event>.event(event: Event) = copy(events = events + event)

fun <State, Event> Transition<State, Event>.events(vararg events: Event) = copy(events = this.events + events)

fun <State, Event> Transition<State, Event>.effect(effect: Effect) = copy(effects = effects + effect)

fun <State, Event> Transition<State, Event>.effects(vararg effects: Effect) = copy(effects = this.effects + effects)