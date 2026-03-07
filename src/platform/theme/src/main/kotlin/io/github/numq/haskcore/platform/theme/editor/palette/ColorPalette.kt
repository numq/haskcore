package io.github.numq.haskcore.platform.theme.editor.palette

interface ColorPalette {
    fun Int.withAlpha(alpha: Float): Int {
        val a = (alpha * 255).toInt().coerceIn(0, 255)

        return ((this.toLong() and 0x00FFFFFFL) or (a.toLong() shl 24)).toInt()
    }
}