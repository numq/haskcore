package io.github.numq.haskcore.common.core.text

interface TextSnapshot {
    val revision: TextRevision

    val encoding: TextEncoding

    val lineEnding: TextLineEnding

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