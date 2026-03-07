package io.github.numq.haskcore.feature.shelf.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.shelf.core.ShelfPanel
import io.github.numq.haskcore.feature.shelf.core.ShelfTool

@Composable
internal fun ShelfPanelContent(panel: ShelfPanel, selectTool: (ShelfTool) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxHeight().width(40.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            panel.tools.forEach { tool ->
                ShelfToolItem(tool = tool, isActive = tool == panel.activeTool, select = {
                    selectTool(tool)
                })
            }
        }
    }
}