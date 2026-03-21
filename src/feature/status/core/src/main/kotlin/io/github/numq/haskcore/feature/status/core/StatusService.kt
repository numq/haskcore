package io.github.numq.haskcore.feature.status.core

import arrow.core.Either

interface StatusService {
    suspend fun getPathSegments(rootPath: String, filePath: String): Either<Throwable, List<String>>
}