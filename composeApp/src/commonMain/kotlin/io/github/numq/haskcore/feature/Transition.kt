package io.github.numq.haskcore.feature

data class Transition<out State>(
    val state: State,
    val events: List<Event> = emptyList(),
) {
    fun withEvents(block: (List<Event>) -> List<Event>) = copy(events = block(events))
}