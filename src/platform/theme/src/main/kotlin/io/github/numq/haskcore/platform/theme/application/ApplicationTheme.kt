package io.github.numq.haskcore.platform.theme.application

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable

@Composable
fun ApplicationTheme(isDark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = when {
            isDark -> DraculaColorScheme

            else -> AlucardColorScheme
        }, typography = Typography(), shapes = Shapes(), content = content
    )
}