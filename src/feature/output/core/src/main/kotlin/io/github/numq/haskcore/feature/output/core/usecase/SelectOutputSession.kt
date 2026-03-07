package io.github.numq.haskcore.feature.output.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.output.core.OutputService

class SelectOutputSession(private val outputService: OutputService) : UseCase<SelectOutputSession.Input, Unit> {
    data class Input(val sessionId: String)

    override suspend fun Raise<Throwable>.execute(input: Input) =
        outputService.selectSession(id = input.sessionId).bind()
}