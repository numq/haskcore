package io.github.numq.haskcore.common.presentation.overlay.menu

import androidx.compose.ui.graphics.vector.ImageVector

data class ContextMenuItem(
    val label: String,
    val leadingIcon: ImageVector? = null,
    val trailingIcon: ImageVector? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)