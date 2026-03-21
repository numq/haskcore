package io.github.numq.haskcore.service.document

import arrow.core.Either
import io.github.numq.haskcore.core.timestamp.Timestamp

interface DocumentService {
    suspend fun getParentPath(path: String): Either<Throwable, String>

    suspend fun getName(path: String): Either<Throwable, String>

    suspend fun getLastModifiedTimestamp(path: String): Either<Throwable, Timestamp>

    suspend fun readDocument(path: String): Either<Throwable, Document>

    suspend fun saveDocument(path: String, content: String): Either<Throwable, Unit>
}