package io.github.numq.haskcore.common.presentation.container

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun ForegroundContainer(modifier: Modifier = Modifier, content: @Composable BoxWithConstraintsScope.() -> Unit) {
    BoxWithConstraints(
        modifier = modifier.background(MaterialTheme.colorScheme.background).clip(RoundedCornerShape(8.dp))
            .padding(4.dp), content = content
    )
}