package io.github.numq.haskcore.feature.editor.presentation.gutter

import org.jetbrains.skia.Rect

internal data class GutterIcon(
    val line: Int, val bounds: Rect, val icon: IconType, val tooltip: String? = null, val onClick: (() -> Unit)? = null
) {
    enum class IconType {
        BOOKMARK, TODO, FIXME, CHANGE, GIT_BLAME, CODE_LENS, ANNOTATION, DIAGNOSTIC
    }
}