package io.github.numq.haskcore.feature.editor.presentation.overlay.error

import org.jetbrains.skia.Rect

internal data class ErrorMarker(
    val line: Int,
    val column: Int,
    val message: String,
    val severity: ErrorSeverity,
    val bounds: Rect,
    val quickFix: QuickFix? = null
)