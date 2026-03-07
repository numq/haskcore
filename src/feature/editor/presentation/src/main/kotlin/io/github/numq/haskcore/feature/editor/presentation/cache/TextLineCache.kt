package io.github.numq.haskcore.feature.editor.presentation.cache

import io.github.numq.haskcore.platform.font.EditorFont
import org.jetbrains.skia.TextLine

internal class TextLineCache(override val capacity: Int) : LruCache<TextLineCache.Key, TextLine>() {
    data class Key(val text: String, val font: EditorFont)

    override val factory: Key.() -> TextLine = {
        font.createTextLine(text = text)
    }
}