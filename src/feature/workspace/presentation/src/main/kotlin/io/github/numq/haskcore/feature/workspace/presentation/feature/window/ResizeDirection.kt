package io.github.numq.haskcore.feature.workspace.presentation.feature.window

import androidx.compose.ui.Alignment

internal enum class ResizeDirection(val alignment: Alignment) {
    TOP(alignment = Alignment.TopCenter), BOTTOM(alignment = Alignment.BottomCenter), LEFT(alignment = Alignment.CenterStart), RIGHT(
        alignment = Alignment.CenterEnd
    ),
    TOP_LEFT(alignment = Alignment.TopStart), TOP_RIGHT(alignment = Alignment.TopEnd), BOTTOM_LEFT(alignment = Alignment.BottomStart), BOTTOM_RIGHT(
        alignment = Alignment.BottomEnd
    )
}