package io.github.numq.haskcore.platform.overlay.dialog.file

interface FileDialog {
    suspend fun pickDirectory(title: String): String?
}