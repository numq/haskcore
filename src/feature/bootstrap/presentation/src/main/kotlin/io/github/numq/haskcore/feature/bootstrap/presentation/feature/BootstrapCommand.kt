package io.github.numq.haskcore.feature.bootstrap.presentation.feature

import io.github.numq.haskcore.common.presentation.font.Font
import io.github.numq.haskcore.feature.bootstrap.core.Bootstrap

internal sealed interface BootstrapCommand {
    enum class Key {
        INITIALIZE
    }

    data class HandleFailure(val throwable: Throwable) : BootstrapCommand

    data object Initialize : BootstrapCommand {
        val key = Key.INITIALIZE
    }

    data class InitializeSuccess(
        val bootstrap: Bootstrap,
        val welcomeLogoFont: Font,
        val welcomeMonoFont: Font,
        val editorMonoFont: Font,
    ) : BootstrapCommand
}