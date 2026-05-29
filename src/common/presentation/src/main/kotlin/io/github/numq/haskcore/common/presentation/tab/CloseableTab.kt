package io.github.numq.haskcore.common.presentation.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CloseableTab(title: String, isSelected: Boolean, select: () -> Unit, close: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }

    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.surface

        isHovered -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .08f)

        else -> Color.Transparent
    }

    Box(
        modifier = Modifier.height(32.dp).clip(RoundedCornerShape(8.dp)).background(backgroundColor)
            .hoverable(interactionSource).clickable(
                interactionSource = interactionSource, indication = null, onClick = select
            ).padding(horizontal = 8.dp), contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title, fontSize = 12.sp, color = when {
                    isSelected -> MaterialTheme.colorScheme.onSurface

                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            when {
                isSelected || isHovered -> {
                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = close, modifier = Modifier.size(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = when {
                                isSelected -> MaterialTheme.colorScheme.onSurface

                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                else -> Spacer(modifier = Modifier.size(20.dp))
            }
        }
    }
}