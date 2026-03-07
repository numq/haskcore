package io.github.numq.haskcore.feature.editor.presentation.cache

import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode

internal class PaintCache(override val capacity: Int) : LruCache<PaintCache.Key, Paint>() {
    data class Key(
        val color: Int,
        val isAntiAlias: Boolean = true,
        val mode: PaintMode = PaintMode.FILL,
        val strokeWidth: Float = 0f,
    )

    override val factory: Key.() -> Paint = {
        val colorKey = this.color

        val isAntiAliasKey = this.isAntiAlias

        val modeKey = this.mode

        val strokeWidthKey = this.strokeWidth

        Paint().apply {
            this.color = colorKey

            this.isAntiAlias = isAntiAliasKey

            this.mode = modeKey

            this.strokeWidth = strokeWidthKey
        }
    }
}