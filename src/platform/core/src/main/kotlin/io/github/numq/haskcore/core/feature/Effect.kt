package io.github.numq.haskcore.core.feature

import kotlinx.coroutines.flow.Flow

sealed interface Effect {
    data class Stream<out Command>(
        val key: Any,
        val flow: Flow<Command>,
        val strategy: Strategy = Strategy.Sequential,
        val fallback: (suspend (Throwable) -> Command)? = null,
    ) : Effect {
        enum class Strategy { Sequential, Restart }
    }

    data class Action<out Command>(
        val key: Any, val fallback: (suspend (Throwable) -> Command)? = null, val block: suspend () -> Command
    ) : Effect

    data class Cancel(val key: Any) : Effect
}