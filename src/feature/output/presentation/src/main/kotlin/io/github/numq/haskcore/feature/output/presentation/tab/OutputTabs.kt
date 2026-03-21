package io.github.numq.haskcore.feature.output.presentation.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.output.core.OutputSession

@Composable
internal fun OutputTabs(
    sessions: List<OutputSession>,
    activeSession: OutputSession?,
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
                    session.id == activeSession?.id
                }.coerceAtLeast(0),
                modifier = Modifier.weight(1f).height(32.dp),
                edgePadding = 0.dp,
                divider = {},
                containerColor = Color.Transparent
            ) {
                sessions.forEach { session ->
                    OutputTab(
                        session = session, isActive = session.id == activeSession?.id, select = select, close = close
                    )
                }
            }
        }
    }
}