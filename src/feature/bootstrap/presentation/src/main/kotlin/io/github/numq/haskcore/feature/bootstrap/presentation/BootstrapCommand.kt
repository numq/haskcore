package io.github.numq.haskcore.feature.bootstrap.presentation

import io.github.numq.haskcore.feature.bootstrap.core.Bootstrap

internal sealed interface BootstrapCommand {
    enum class Key {
        INITIALIZE
    }

    data class HandleFailure(val throwable: Throwable) : BootstrapCommand

    data object Initialize : BootstrapCommand {
        val key = Key.INITIALIZE
    }

    data class UpdateBootstrap(val bootstrap: Bootstrap) : BootstrapCommand
}