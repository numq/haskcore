package io.github.numq.haskcore.service.project

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

interface ProjectService {
    val project: Flow<Project>

    suspend fun renameProject(name: String): Either<Throwable, Unit>

    suspend fun openDocument(path: String): Either<Throwable, Unit>

    suspend fun closeDocument(path: String): Either<Throwable, Unit>
}