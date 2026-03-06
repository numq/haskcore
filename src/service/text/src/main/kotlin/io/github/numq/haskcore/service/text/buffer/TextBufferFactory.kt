package io.github.numq.haskcore.service.text.buffer

import arrow.core.Either

internal interface TextBufferFactory {
    suspend fun create(text: String): Either<Throwable, TextBuffer>
}