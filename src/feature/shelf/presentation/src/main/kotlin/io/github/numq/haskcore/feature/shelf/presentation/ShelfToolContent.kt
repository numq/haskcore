package io.github.numq.haskcore.feature.shelf.presentation

import androidx.compose.runtime.Composable
import io.github.numq.haskcore.feature.shelf.core.ShelfTool

@Composable
internal fun ShelfToolContent(tool: ShelfTool, explorer: @Composable () -> Unit, log: @Composable () -> Unit) {
    when (tool) {
        is ShelfTool.Explorer -> explorer()

        is ShelfTool.Log -> log()
    }
}