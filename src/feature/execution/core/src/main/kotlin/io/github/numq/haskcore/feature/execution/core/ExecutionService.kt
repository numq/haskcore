package io.github.numq.haskcore.feature.execution.core

import arrow.core.Either
import kotlinx.coroutines.flow.StateFlow

interface ExecutionService : AutoCloseable {
    val fileSeparator: String

    val configurations: StateFlow<List<ExecutionConfiguration>>

    val selectedConfiguration: StateFlow<ExecutionConfiguration?>

    suspend fun addConfiguration(configuration: ExecutionConfiguration): Either<Throwable, Unit>

    suspend fun updateConfiguration(configuration: ExecutionConfiguration): Either<Throwable, Unit>

    suspend fun removeConfiguration(id: String): Either<Throwable, Unit>

    suspend fun setCurrentConfiguration(id: String?): Either<Throwable, Unit>
}