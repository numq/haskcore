package io.github.numq.haskcore.common.presentation.overlay.dialog.file

interface FileDialog {
    suspend fun pickDirectory(title: String): String?
}