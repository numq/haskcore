package io.github.numq.haskcore.feature.output.presentation.feature

import io.github.numq.haskcore.feature.output.core.Output
import io.github.numq.haskcore.feature.output.presentation.menu.ContextMenuState

data class OutputState(
    val output: Output = Output(), val contextMenuState: ContextMenuState = ContextMenuState.Hidden
)