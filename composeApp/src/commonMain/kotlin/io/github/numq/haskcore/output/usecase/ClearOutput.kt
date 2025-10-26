package io.github.numq.haskcore.output.usecase

import io.github.numq.haskcore.output.OutputRepository
import io.github.numq.haskcore.usecase.UseCase

internal class ClearOutput(private val outputRepository: OutputRepository) : UseCase<Unit, Unit> {
    override suspend fun execute(input: Unit) = outputRepository.clear()
}