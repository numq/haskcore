package io.github.numq.haskcore.feature.editor.presentation.overlay.error

internal data class QuickFix(val title: String, val action: () -> Unit)