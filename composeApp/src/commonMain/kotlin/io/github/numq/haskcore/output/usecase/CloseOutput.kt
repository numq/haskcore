package io.github.numq.haskcore.output.usecase

import io.github.numq.haskcore.output.OutputRepository
import io.github.numq.haskcore.output.Output
import io.github.numq.haskcore.usecase.UseCase

internal class CloseOutput(
    private val outputRepository: OutputRepository
) : UseCase<CloseOutput.Input, Unit> {
    data class Input(val output: Output)

    override suspend fun execute(input: Input) = outputRepository.close(output = input.output)
}