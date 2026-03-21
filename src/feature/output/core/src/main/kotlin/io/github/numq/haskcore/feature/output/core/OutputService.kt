package io.github.numq.haskcore.feature.output.core

import arrow.core.Either
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

interface OutputService : AutoCloseable {
    val output: StateFlow<Output>

    suspend fun openSession(id: String): Either<Throwable, Unit>

    suspend fun closeSession(id: String): Either<Throwable, Unit>

    suspend fun startSession(id: String, name: String, configuration: String): Either<Throwable, Unit>

    suspend fun stopSession(id: String, exitCode: Int, duration: Duration): Either<Throwable, Unit>

    suspend fun push(id: String, line: OutputLine): Either<Throwable, Unit>
}