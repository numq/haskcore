package io.github.numq.haskcore.service.text.buffer

import arrow.core.right

internal class RopeTextBufferFactory : TextBufferFactory {
    override suspend fun create(text: String) = RopeTextBuffer(initialText = text).right()
}