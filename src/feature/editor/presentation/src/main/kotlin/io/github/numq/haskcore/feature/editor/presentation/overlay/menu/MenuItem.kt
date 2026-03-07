package io.github.numq.haskcore.feature.editor.presentation.overlay.menu

import io.github.numq.haskcore.feature.editor.presentation.gutter.GutterIcon

internal data class MenuItem(
    val title: String, val icon: GutterIcon.IconType? = null, val enabled: Boolean = true, val onClick: () -> Unit
)