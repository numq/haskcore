package io.github.numq.haskcore.feature.output.presentation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.output.core.OutputLine

@Composable
internal fun OutputLineItem(line: OutputLine) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        val (prefix, color) = when (line) {
            is OutputLine.Error -> "[E] " to MaterialTheme.colorScheme.error

            is OutputLine.System -> "[S] " to MaterialTheme.colorScheme.primary

            else -> "[I] " to MaterialTheme.colorScheme.onSurfaceVariant
        }

        Text(
            text = prefix, style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
            ), color = color, modifier = Modifier.width(24.dp)
        )

        Text(
            text = line.text,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}