package io.github.numq.haskcore.feature.editor.presentation.menu

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
internal fun ContextMenuIcon(imageVector: ImageVector) {
    Icon(imageVector = imageVector, contentDescription = null, modifier = Modifier.size(18.dp))
}