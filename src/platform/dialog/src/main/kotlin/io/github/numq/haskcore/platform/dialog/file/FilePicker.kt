package io.github.numq.haskcore.platform.dialog.file

interface FilePicker {
    suspend fun pickDirectory(title: String): String?
}