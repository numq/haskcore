package io.github.numq.haskcore.menu

internal data class ContextMenuAction(val label: String, val enabled: Boolean = true, val click: () -> Unit)