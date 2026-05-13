package io.github.numq.haskcore.common.presentation.container

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BackgroundContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        content()
    }
}