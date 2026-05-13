package io.github.numq.haskcore.feature.execution.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.execution.core.ExecutionConfiguration
import io.github.numq.haskcore.feature.execution.core.ExecutionService

class SetCurrentConfiguration(
    private val executionService: ExecutionService,
) : UseCase<SetCurrentConfiguration.Input, Unit> {
    data class Input(val configuration: ExecutionConfiguration)

    override suspend fun Raise<Throwable>.execute(input: Input) = with(input.configuration) {
        executionService.setCurrentConfiguration(id = id).bind()
    }
}