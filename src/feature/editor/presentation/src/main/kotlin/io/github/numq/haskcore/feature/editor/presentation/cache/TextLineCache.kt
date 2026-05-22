package io.github.numq.haskcore.feature.editor.presentation.cache

import io.github.numq.haskcore.common.presentation.font.Font
import org.jetbrains.skia.TextLine

internal class TextLineCache(override val capacity: Int) : LruCache<TextLineCache.Key, TextLine>() {
    data class Key(val text: String, val font: Font)

    override val factory: Key.() -> TextLine = {
        font.createTextLine(text = text)
    }
}