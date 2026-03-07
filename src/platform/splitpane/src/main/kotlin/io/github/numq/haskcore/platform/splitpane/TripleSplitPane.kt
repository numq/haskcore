package io.github.numq.haskcore.platform.splitpane

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun TripleSplitPane(
    modifier: Modifier,
    leftWeight: Float,
    rightWeight: Float,
    onLeftResize: (Float) -> Unit,
    onRightResize: (Float) -> Unit,
    leftContent: @Composable () -> Unit,
    rightContent: @Composable () -> Unit,
    centerContent: @Composable () -> Unit,
) {
    val centerWeight by remember(leftWeight, rightWeight) {
        derivedStateOf {
            (1f - leftWeight - rightWeight).coerceAtLeast(.2f)
        }
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val totalWidth = constraints.maxWidth.toFloat()

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leftWeight > 0f) {
                Box(modifier = Modifier.weight(leftWeight)) {
                    leftContent()
                }

                VerticalHandle(totalWidth = totalWidth, onPositionChange = { deltaX ->
                    if (totalWidth > 0f) {
                        onLeftResize(deltaX / totalWidth)
                    }
                })
            }

            Box(modifier = Modifier.weight(centerWeight)) {
                centerContent()
            }

            if (rightWeight > 0f) {
                VerticalHandle(totalWidth = totalWidth, onPositionChange = { deltaX ->
                    if (totalWidth > 0f) {
                        onRightResize(-deltaX / totalWidth)
                    }
                })

                Box(modifier = Modifier.weight(rightWeight)) {
                    rightContent()
                }
            }
        }
    }
}