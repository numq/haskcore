package io.github.numq.haskcore.feature.editor.presentation.feature.view

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.editor.presentation.hint.ShortcutHint

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
                verticalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.CenterVertically)
            ) {
                ShortcutHint(label = "Open Project", keys = "Ctrl + O") // todo add shortcut

                ShortcutHint(label = "New File", keys = "Ctrl + N") // todo add shortcut

                ShortcutHint(label = "Search Everywhere", keys = "Double Shift") // todo add shortcut
            }
        }
    }
}