package io.github.numq.haskcore.contextmenu

import androidx.compose.foundation.ContextMenuItem

internal class DisableableContextMenuItem(
    label: String,
    val click: () -> Unit,
    val enabled: Boolean = true
) : ContextMenuItem(label = label, onClick = { if (enabled) click() })