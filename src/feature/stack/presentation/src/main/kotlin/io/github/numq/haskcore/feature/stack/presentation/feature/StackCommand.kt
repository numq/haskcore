package io.github.numq.haskcore.feature.stack.presentation.feature

import io.github.numq.haskcore.common.core.log.Log
import kotlinx.coroutines.flow.Flow

internal sealed interface StackCommand {
    enum class Key {
        INITIALIZE, INITIALIZE_SUCCESS, CLEAR
    }

    data class HandleFailure(val throwable: Throwable) : StackCommand

    data object Initialize : StackCommand {
        val key = Key.INITIALIZE
    }

    data class InitializeSuccess(val flow: Flow<List<Log>>) : StackCommand {
        val key = Key.INITIALIZE_SUCCESS
    }

    data class UpdateLogs(val logs: List<Log>) : StackCommand

    data object Clear : StackCommand {
        val key = Key.CLEAR
    }

    data object ClearSuccess : StackCommand
}