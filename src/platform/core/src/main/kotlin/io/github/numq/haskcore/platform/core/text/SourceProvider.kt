package io.github.numq.haskcore.platform.core.text

import kotlinx.coroutines.flow.SharedFlow

interface SourceProvider {
    val text: String

    val edits: SharedFlow<TextEdit>

    fun getBytePosition(position: TextPosition): Int?

    fun getTextPosition(bytePosition: Int): TextPosition?

    fun getTextInRange(range: TextRange): String

    fun getLineText(line: Int): String
}