package io.github.numq.haskcore.output.usecase

import io.github.numq.haskcore.output.OutputRepository
import io.github.numq.haskcore.usecase.UseCase

internal class ClearOutput(
    private val outputRepository: OutputRepository,
) : UseCase<ClearOutput.Input, Unit> {
    data class Input(val id: String)

    override suspend fun execute(input: Input) = outputRepository.clear(id = input.id)
}