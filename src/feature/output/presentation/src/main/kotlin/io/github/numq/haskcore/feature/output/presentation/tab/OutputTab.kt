package io.github.numq.haskcore.feature.output.presentation.tab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.output.core.OutputSession

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun OutputTab(
    session: OutputSession, isActive: Boolean, select: (OutputSession) -> Unit, close: (OutputSession) -> Unit
) {
    Tab(selected = isActive, onClick = { select(session) }, modifier = Modifier.height(32.dp), text = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 6.dp, alignment = Alignment.Start),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = session.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = when {
                    isActive -> FontWeight.Bold

                    else -> FontWeight.Normal
                },
                color = when {
                    isActive -> MaterialTheme.colorScheme.onSurface

                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Box(
                modifier = Modifier.size(20.dp).clip(CircleShape).clickable(onClick = { close(session) }),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    })
}