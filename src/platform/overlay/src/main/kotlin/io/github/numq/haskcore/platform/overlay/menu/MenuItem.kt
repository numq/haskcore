package io.github.numq.haskcore.platform.overlay.menu

import androidx.compose.runtime.Composable

data class MenuItem(
    val label: String,
    val leadingIcon: (@Composable () -> Unit)? = null,
    val trailingIcon: (@Composable () -> Unit)? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)