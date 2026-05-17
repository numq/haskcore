package io.github.numq.haskcore.feature.workspace.presentation.shelf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.workspace.core.ShelfPanel
import io.github.numq.haskcore.feature.workspace.core.ShelfTool

@Composable
internal fun ShelfPanelContent(panel: ShelfPanel, selectTool: (ShelfTool) -> Unit) {
    Column(
        modifier = Modifier.fillMaxHeight().width(40.dp),
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