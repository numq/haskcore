package io.github.numq.haskcore.output.usecase

import io.github.numq.haskcore.output.OutputLine
import io.github.numq.haskcore.output.OutputRepository
import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow

internal class ObserveOutput(
    private val outputRepository: OutputRepository,
) : UseCase<ObserveOutput.Input, Flow<List<OutputLine>>> {
    data class Input(val id: String)

    override suspend fun execute(input: Input) = outputRepository.observe(id = input.id)
}