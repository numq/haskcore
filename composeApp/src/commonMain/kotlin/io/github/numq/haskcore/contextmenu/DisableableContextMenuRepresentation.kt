package io.github.numq.haskcore.contextmenu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuRepresentation
import androidx.compose.foundation.ContextMenuState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

internal object DisableableContextMenuRepresentation : ContextMenuRepresentation {
    @Composable
    override fun Representation(state: ContextMenuState, items: () -> List<ContextMenuItem>) {
        val list = items()

        if (state.status !is ContextMenuState.Status.Open) return

        DropdownMenu(expanded = true, onDismissRequest = { state.status = ContextMenuState.Status.Closed }) {
            list.forEach { item ->
                when (item) {
                    is DisableableContextMenuItem -> DropdownMenuItem(
                        text = { Text(text = item.label, style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier.alpha(if (item.enabled) 1f else .38f),
                        onClick = { if (item.enabled) item.onClick() },
                        enabled = item.enabled
                    )

                    else -> DropdownMenuItem(
                        text = { Text(text = item.label, style = MaterialTheme.typography.bodyMedium) },
                        onClick = item.onClick
                    )
                }
            }
        }
    }
}