package io.github.numq.haskcore.feature.bootstrap.presentation.feature

import io.github.numq.haskcore.common.presentation.font.Font
import io.github.numq.haskcore.feature.bootstrap.core.Bootstrap

internal sealed interface BootstrapState {
    data object Active : BootstrapState

    data class Content(
        val bootstrap: Bootstrap, val welcomeLogoFont: Font, val welcomeMonoFont: Font, val editorMonoFont: Font,
    ) : BootstrapState
}