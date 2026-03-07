package io.github.numq.haskcore.feature.editor.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.editor.presentation.placeholder.ShortcutHint

@Composable
internal fun EditorViewEmpty() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .1f)
            )

            Column(
                modifier = Modifier.wrapContentWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShortcutHint(label = "Open Project", keys = "Ctrl + O")

                ShortcutHint(label = "New File", keys = "Ctrl + N")

                ShortcutHint(label = "Search Everywhere", keys = "Double Shift")
            }
        }
    }
}