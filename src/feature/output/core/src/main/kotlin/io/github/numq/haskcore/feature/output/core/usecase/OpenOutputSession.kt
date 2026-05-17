package io.github.numq.haskcore.feature.output.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.output.core.OutputService

class OpenOutputSession(private val outputService: OutputService) : UseCase.Command<OpenOutputSession.Input> {
    data class Input(val sessionId: String)

    override suspend fun Raise<Throwable>.command(input: Input) = outputService.openSession(id = input.sessionId).bind()
}