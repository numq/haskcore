package io.github.numq.haskcore.feature.status.presentation

import io.github.numq.haskcore.feature.status.core.Status
import kotlinx.coroutines.flow.Flow

internal sealed interface StatusCommand {
    enum class Key {
        INITIALIZE, INITIALIZE_SUCCESS, RESET_CABAL_PATH, RESET_GHC_PATH, RESET_STACK_PATH, RESET_HLS_PATH, SELECT_CABAL_PATH, SELECT_GHC_PATH, SELECT_STACK_PATH, SELECT_HLS_PATH
    }

    data class HandleFailure(val throwable: Throwable) : StatusCommand

    data object Initialize : StatusCommand {
        val key = Key.INITIALIZE
    }

    data class InitializeSuccess(val flow: Flow<Status>) : StatusCommand {
        val key = Key.INITIALIZE_SUCCESS
    }

    data class UpdateStatus(val status: Status) : StatusCommand

    data object ResetGhcPath : StatusCommand {
        val key = Key.RESET_GHC_PATH
    }

    data object ResetGhcPathSuccess : StatusCommand

    data object ResetCabalPath : StatusCommand {
        val key = Key.RESET_CABAL_PATH
    }

    data object ResetCabalPathSuccess : StatusCommand

    data object ResetStackPath : StatusCommand {
        val key = Key.RESET_STACK_PATH
    }

    data object ResetStackPathSuccess : StatusCommand

    data object ResetHlsPath : StatusCommand {
        val key = Key.RESET_HLS_PATH
    }

    data object ResetHlsPathSuccess : StatusCommand

    data class SelectGhcPath(val path: String) : StatusCommand {
        val key = Key.SELECT_GHC_PATH
    }

    data object SelectGhcPathSuccess : StatusCommand

    data class SelectCabalPath(val path: String) : StatusCommand {
        val key = Key.SELECT_CABAL_PATH
    }

    data object SelectCabalPathSuccess : StatusCommand

    data class SelectStackPath(val path: String) : StatusCommand {
        val key = Key.SELECT_STACK_PATH
    }

    data object SelectStackPathSuccess : StatusCommand

    data class SelectHlsPath(val path: String) : StatusCommand {
        val key = Key.SELECT_HLS_PATH
    }

    data object SelectHlsPathSuccess : StatusCommand
}