package io.github.numq.haskcore.platform.dialog

interface FilePicker {
    suspend fun pickDirectory(title: String): String?
}