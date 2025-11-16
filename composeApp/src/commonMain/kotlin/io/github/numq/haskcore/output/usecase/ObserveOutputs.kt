package io.github.numq.haskcore.output.usecase

import io.github.numq.haskcore.output.OutputRepository
import io.github.numq.haskcore.output.Output
import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow

internal class ObserveOutputs(
    private val outputRepository: OutputRepository
) : UseCase<Unit, Flow<List<Output>>> {
    override suspend fun execute(input: Unit) = Result.success(outputRepository.outputs)
}