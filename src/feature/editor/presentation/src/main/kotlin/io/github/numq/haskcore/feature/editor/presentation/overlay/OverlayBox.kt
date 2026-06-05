package io.github.numq.haskcore.feature.editor.presentation.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun OverlayBox(
    modifier: Modifier, backgroundColor: Color, borderColor: Color, content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.background(backgroundColor).border(1.dp, borderColor).padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}