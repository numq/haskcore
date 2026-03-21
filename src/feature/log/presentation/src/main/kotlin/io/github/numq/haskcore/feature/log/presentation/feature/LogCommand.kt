package io.github.numq.haskcore.feature.log.presentation.feature

import io.github.numq.haskcore.core.log.Log
import kotlinx.coroutines.flow.Flow

internal sealed interface LogCommand {
    enum class Key {
        INITIALIZE, INITIALIZE_SUCCESS
    }

    data class HandleFailure(val throwable: Throwable) : LogCommand

    data object Initialize : LogCommand {
        val key = Key.INITIALIZE
    }

    data class InitializeSuccess(val flow: Flow<List<Log>>) : LogCommand {
        val key = Key.INITIALIZE_SUCCESS
    }

    data class UpdateLogs(val logs: List<Log>) : LogCommand
}