package io.github.numq.haskcore.feature.output.presentation

import io.github.numq.haskcore.feature.output.core.Output
import kotlinx.coroutines.flow.Flow

internal sealed interface OutputCommand {
    enum class Key {
        INITIALIZE, INITIALIZE_SUCCESS, SELECT_SESSION, CLOSE_SESSION
    }

    data class HandleFailure(val throwable: Throwable) : OutputCommand

    data object Initialize : OutputCommand {
        val key = Key.INITIALIZE
    }

    data class InitializeSuccess(val flow: Flow<Output>) : OutputCommand {
        val key = Key.INITIALIZE_SUCCESS
    }

    data class UpdateOutput(val output: Output) : OutputCommand

    data class SelectSession(val sessionId: String) : OutputCommand {
        val key = Key.SELECT_SESSION
    }

    data object SelectSessionSuccess : OutputCommand

    data class CloseSession(val sessionId: String) : OutputCommand {
        val key = Key.CLOSE_SESSION
    }

    data object CloseSessionSuccess : OutputCommand
}