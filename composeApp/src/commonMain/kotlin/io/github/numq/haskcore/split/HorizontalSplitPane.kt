package io.github.numq.haskcore.split

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope

@Composable
fun WindowScope.HorizontalSplitPane(
    percentage: Float,
    onPercentageChange: (Float) -> Unit,
    modifier: Modifier,
    thickness: Dp = 4.dp,
    minPercentage: Float = 0f,
    maxPercentage: Float = 1f,
    enabled: Boolean = true,
    first: @Composable (size: Dp) -> Unit,
    second: @Composable (size: Dp) -> Unit,
) = SplitPane(
    orientation = SplitPaneOrientation.HORIZONTAL,
    percentage = percentage,
    onPercentageChange = onPercentageChange,
    modifier = modifier,
    thickness = thickness,
    minPercentage = minPercentage,
    maxPercentage = maxPercentage,
    enabled = enabled,
    first = first,
    second = second,
)