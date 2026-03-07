package io.github.numq.haskcore.feature.output.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.output.core.OutputSession

@Composable
internal fun OutputTabs(
    sessions: List<OutputSession>,
    selectedSession: OutputSession?,
    select: (OutputSession) -> Unit,
    close: (OutputSession) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(32.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .3f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "OUTPUT",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (sessions.isNotEmpty()) {
            SecondaryScrollableTabRow(
                selectedTabIndex = sessions.indexOfFirst { session ->
                    session.id == selectedSession?.id
                }.coerceAtLeast(0),
                modifier = Modifier.weight(1f).height(32.dp),
                edgePadding = 0.dp,
                containerColor = Color.Transparent
            ) {
                sessions.forEach { session ->
                    val isSelected = session.id == selectedSession?.id

                    Tab(
                        selected = isSelected,
                        onClick = { select(session) },
                        modifier = Modifier.height(32.dp),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(6.dp).background(
                                        color = when (session) {
                                            is OutputSession.Active -> MaterialTheme.colorScheme.primary

                                            is OutputSession.Completed -> when (session.exitCode) {
                                                0 -> Color(0xFF4CAF50)

                                                else -> Color(0xFFF44336)
                                            }
                                        }, shape = CircleShape
                                    )
                                )

                                Text(
                                    text = session.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = when {
                                        isSelected -> FontWeight.Bold

                                        else -> FontWeight.Normal
                                    },
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onSurface

                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )

                                if (session is OutputSession.Completed) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        modifier = Modifier.size(14.dp).clip(CircleShape).clickable(onClick = {
                                            close(session)
                                        }),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .6f)
                                    )
                                }
                            }
                        })
                }
            }
        }
    }
}