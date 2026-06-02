package io.github.numq.haskcore.service.document

import arrow.core.Either
import io.github.numq.haskcore.common.core.text.TextEncoding

interface DocumentService {
    suspend fun readMetadata(path: String): Either<Throwable, Metadata>

    suspend fun readDocument(path: String): Either<Throwable, Document>

    suspend fun saveDocument(path: String, content: String, encoding: TextEncoding): Either<Throwable, Unit>
}