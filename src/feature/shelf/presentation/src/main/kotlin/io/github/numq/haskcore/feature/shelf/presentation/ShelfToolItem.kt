package io.github.numq.haskcore.feature.shelf.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.feature.shelf.core.ShelfTool

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ShelfToolItem(tool: ShelfTool, isActive: Boolean, select: () -> Unit) {
    val backgroundColor = when {
        isActive -> MaterialTheme.colorScheme.secondaryContainer

        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        isActive -> MaterialTheme.colorScheme.onSecondaryContainer

        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        IconButton(
            onClick = select,
            modifier = Modifier.size(32.dp).padding(2.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = backgroundColor, contentColor = contentColor
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            val icon = when (tool) {
                is ShelfTool.Explorer -> Icons.Outlined.Folder

                is ShelfTool.Log -> Icons.Outlined.Info
            }

            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}