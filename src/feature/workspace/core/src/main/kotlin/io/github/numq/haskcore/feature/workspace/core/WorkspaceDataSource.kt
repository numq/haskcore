package io.github.numq.haskcore.feature.workspace.core

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

internal interface WorkspaceDataSource : AutoCloseable {
    val workspaceData: Flow<WorkspaceData>

    suspend fun get(): Either<Throwable, WorkspaceData>

    suspend fun update(transform: (WorkspaceData) -> WorkspaceData): Either<Throwable, WorkspaceData>
}