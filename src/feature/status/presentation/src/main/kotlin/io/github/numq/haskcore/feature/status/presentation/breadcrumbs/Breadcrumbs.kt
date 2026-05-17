package io.github.numq.haskcore.feature.status.presentation.breadcrumbs

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.status.presentation.StatusItem
import java.io.File

@Composable
internal fun Breadcrumbs(modifier: Modifier = Modifier, pathSegments: List<String>, navigateToPath: (String) -> Unit) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        pathSegments.forEachIndexed { index, path ->
            val isLast = index == pathSegments.lastIndex

            val name = File(path).name.ifEmpty { path }

            StatusItem(onClick = { navigateToPath(path) }) {
                Text(
                    text = name, fontWeight = when {
                        isLast -> FontWeight.SemiBold

                        else -> FontWeight.Normal
                    }, style = MaterialTheme.typography.labelMedium, color = when {
                        isLast -> MaterialTheme.colorScheme.primary

                        else -> Color.Unspecified
                    }
                )
            }

            if (!isLast) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = .5f)
                )
            }
        }
    }
}