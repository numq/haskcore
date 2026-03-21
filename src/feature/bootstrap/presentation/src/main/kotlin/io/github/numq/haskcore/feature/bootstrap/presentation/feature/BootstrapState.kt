package io.github.numq.haskcore.feature.bootstrap.presentation.feature

import io.github.numq.haskcore.feature.bootstrap.core.Bootstrap

internal sealed interface BootstrapState {
    data object Active : BootstrapState

    data class Content(val bootstrap: Bootstrap) : BootstrapState
}