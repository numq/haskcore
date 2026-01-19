package io.github.numq.haskcore.core.feature

import kotlinx.coroutines.flow.Flow

sealed interface Effect {
    data class Notify<out Payload>(val data: Payload) : Effect

    data class Collect<out Command>(
        val key: Any,
        val strategy: Strategy = Strategy.Sequential,
        val flow: Flow<Command>,
        val fallback: suspend (Throwable) -> Command
    ) : Effect {
        enum class Strategy { Sequential, Restart }
    }

    data class Execute<out Command>(
        val key: Any, val block: suspend () -> Command, val fallback: suspend (Throwable) -> Command
    ) : Effect

    data class Cancel(val key: Any) : Effect
}