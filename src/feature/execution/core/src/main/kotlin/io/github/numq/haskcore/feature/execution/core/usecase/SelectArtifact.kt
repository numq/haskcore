package io.github.numq.haskcore.feature.execution.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.execution.core.ExecutionArtifact
import io.github.numq.haskcore.feature.execution.core.ExecutionService

class SelectArtifact(private val executionService: ExecutionService) : UseCase<SelectArtifact.Input, Unit> {
    data class Input(val artifact: ExecutionArtifact)

    override suspend fun Raise<Throwable>.execute(input: Input) =
        executionService.selectArtifact(artifact = input.artifact).bind()
}