package io.github.numq.haskcore.core.text

import java.nio.charset.Charset

interface TextSnapshot {
    val revision: Long

    val charset: Charset

    val lineEnding: LineEnding

    val lines: Int

    val maxLineLength: Int

    val lastPosition: TextPosition

    val text: String

    fun isValidPosition(position: TextPosition): Boolean

    fun getLineText(line: Int): String

    fun getLineLength(line: Int): Int

    fun getTextInRange(range: TextRange): String

    fun getBytePosition(position: TextPosition): Int?

    fun getTextPosition(bytePosition: Int): TextPosition?
}