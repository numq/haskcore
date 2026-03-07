package io.github.numq.haskcore.feature.execution.core

import arrow.core.Either
import kotlinx.coroutines.flow.StateFlow

interface ExecutionService : AutoCloseable {
    val selectedArtifactPath: StateFlow<String?>

    suspend fun selectArtifact(artifact: ExecutionArtifact?): Either<Throwable, Unit>
}