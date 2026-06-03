package io.github.numq.haskcore.service.text.buffer

import arrow.core.right
import io.github.numq.haskcore.common.core.text.TextEncoding
import io.github.numq.haskcore.common.core.text.TextLineEnding

internal class RopeTextBufferFactory : TextBufferFactory {
    override suspend fun create(
        text: String, encoding: TextEncoding, lineEnding: TextLineEnding,
    ) = RopeTextBuffer(initialText = text, initialEncoding = encoding, initialLineEnding = lineEnding).right()
}