package io.github.numq.haskcore.feature.execution.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.execution.core.ExecutionArtifact
import io.github.numq.haskcore.service.runtime.RuntimeRequest
import io.github.numq.haskcore.service.runtime.RuntimeService
import java.util.*

class StartExecution(private val runtimeService: RuntimeService) : UseCase<StartExecution.Input, Unit> {
    data class Input(val artifact: ExecutionArtifact)

    override suspend fun Raise<Throwable>.execute(input: Input) = with(input) {
        val id = UUID.randomUUID().toString()

        val name = artifact.target.name

        val request = when (artifact) {
            is ExecutionArtifact.Cabal -> RuntimeRequest.Cabal(
                id = id, name = name, arguments = listOf("run", artifact.target.name)
            )

            is ExecutionArtifact.Stack -> RuntimeRequest.Stack(
                id = id, name = name, arguments = listOf("run", artifact.target.name)
            )

            is ExecutionArtifact.File -> RuntimeRequest.RunGHC(
                id = id, name = name, path = artifact.target.path, arguments = emptyList()
            )
        }

        runtimeService.start(request = request).bind()
    }
}