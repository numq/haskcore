package io.github.numq.haskcore.feature.execution.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.service.runtime.RuntimeService
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.execution.core.ExecutionConfiguration

class StopConfiguration(private val runtimeService: RuntimeService) : UseCase<StopConfiguration.Input, Unit> {
    data class Input(val configuration: ExecutionConfiguration)

    override suspend fun Raise<Throwable>.execute(input: Input) = with(input.configuration) {
        runtimeService.stop(id = id).bind()
    }
}