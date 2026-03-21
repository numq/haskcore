package io.github.numq.haskcore.feature.output.presentation.feature

import io.github.numq.haskcore.feature.output.core.Output
import io.github.numq.haskcore.feature.output.core.OutputSession
import kotlinx.coroutines.flow.Flow

internal sealed interface OutputCommand {
    enum class Key {
        INITIALIZE, INITIALIZE_SUCCESS, SELECT_SESSION, CLOSE_SESSION, COPY_TEXT
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

    data class OpenMenu(val x: Float, val y: Float) : OutputCommand

    data object CloseMenu : OutputCommand

    data class CopyText(val session: OutputSession) : OutputCommand {
        val key = Key.COPY_TEXT
    }

    data object CopyTextSuccess : OutputCommand
}