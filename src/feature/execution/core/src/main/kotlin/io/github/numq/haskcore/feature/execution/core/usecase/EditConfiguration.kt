package io.github.numq.haskcore.feature.execution.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.execution.core.ExecutionConfiguration
import io.github.numq.haskcore.feature.execution.core.ExecutionService

class EditConfiguration(private val executionService: ExecutionService) : UseCase<EditConfiguration.Input, Unit> {
    data class Input(val configuration: ExecutionConfiguration)

    override suspend fun Raise<Throwable>.execute(input: Input) = with(input) {
        executionService.updateConfiguration(configuration = configuration).bind()
    }
}