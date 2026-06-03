package io.github.numq.haskcore.service.text.buffer

import arrow.core.Either
import io.github.numq.haskcore.common.core.text.TextEncoding
import io.github.numq.haskcore.common.core.text.TextLineEnding

internal interface TextBufferFactory {
    suspend fun create(text: String, encoding: TextEncoding, lineEnding: TextLineEnding): Either<Throwable, TextBuffer>
}