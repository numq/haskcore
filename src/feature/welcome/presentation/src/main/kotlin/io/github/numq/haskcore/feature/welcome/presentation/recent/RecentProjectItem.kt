package io.github.numq.haskcore.feature.welcome.presentation.recent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.welcome.core.RecentProject

@Composable
internal fun RecentProjectItem(recentProject: RecentProject, openProject: () -> Unit, removeFromHistory: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = openProject),
        color = Color.Transparent,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(8.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.CenterVertically),
            ) {
                when (val name = recentProject.name) {
                    null -> Text(text = recentProject.path, style = MaterialTheme.typography.titleMedium)

                    else -> {
                        Text(text = name, style = MaterialTheme.typography.titleMedium)

                        Text(
                            text = recentProject.path,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(onClick = removeFromHistory, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = null)
            }
        }
    }
}