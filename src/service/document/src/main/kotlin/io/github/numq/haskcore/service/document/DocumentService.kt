package io.github.numq.haskcore.service.document

import arrow.core.Either

interface DocumentService {
    suspend fun readDocument(path: String): Either<Throwable, Document>

    suspend fun saveDocument(path: String, content: String): Either<Throwable, Unit>
}