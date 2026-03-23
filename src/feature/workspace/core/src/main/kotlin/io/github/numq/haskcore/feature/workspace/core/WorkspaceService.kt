package io.github.numq.haskcore.feature.workspace.core

import arrow.core.Either
import kotlinx.coroutines.flow.StateFlow

interface WorkspaceService : AutoCloseable {
    val workspace: StateFlow<Workspace>

    suspend fun getName(path: String): Either<Throwable, String>

    suspend fun saveDimensions(
        x: Float, y: Float, width: Float, height: Float, isFullscreen: Boolean
    ): Either<Throwable, Unit>

    suspend fun saveRatio(ratio: Float): Either<Throwable, Unit>
}