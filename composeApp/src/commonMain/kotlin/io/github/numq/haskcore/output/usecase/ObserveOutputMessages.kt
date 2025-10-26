package io.github.numq.haskcore.output.usecase

import io.github.numq.haskcore.output.OutputMessage
import io.github.numq.haskcore.output.OutputRepository
import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow

internal class ObserveOutputMessages(
    private val outputRepository: OutputRepository
) : UseCase<Unit, Flow<List<OutputMessage>>> {
    override suspend fun execute(input: Unit) = Result.success(outputRepository.messages)
}