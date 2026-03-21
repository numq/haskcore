package io.github.numq.haskcore.feature.status.presentation.breadcrumbs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
internal fun Breadcrumbs(modifier: Modifier = Modifier, pathSegments: List<String>, navigateToPath: (String) -> Unit) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        pathSegments.forEachIndexed { index, path ->
            val isLast = index == pathSegments.lastIndex

            val name = File(path).name.ifEmpty { path }

            Surface(
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { navigateToPath(path) },
                color = Color.Transparent,
                contentColor = when {
                    isLast -> MaterialTheme.colorScheme.primary

                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            ) {
                Text(
                    text = name, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = when {
                        isLast -> FontWeight.SemiBold

                        else -> FontWeight.Normal
                    }, style = MaterialTheme.typography.bodySmall
                )
            }

            if (!isLast) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                )
            }
        }
    }
}