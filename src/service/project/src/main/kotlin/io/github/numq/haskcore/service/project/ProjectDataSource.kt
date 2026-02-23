package io.github.numq.haskcore.service.project

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

internal interface ProjectDataSource : AutoCloseable {
    val projectData: Flow<ProjectData>

    suspend fun get(): Either<Throwable, ProjectData>

    suspend fun update(transform: (ProjectData) -> ProjectData): Either<Throwable, ProjectData>
}