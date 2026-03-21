package io.github.numq.haskcore.feature.output.presentation.line

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
    val (prefix, color) = when (line) {
        is OutputLine.Error -> "[E] " to MaterialTheme.colorScheme.error

        is OutputLine.System -> "[S] " to MaterialTheme.colorScheme.primary

        else -> "[I] " to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.Start),
        verticalAlignment = Alignment.Top
    ) {
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