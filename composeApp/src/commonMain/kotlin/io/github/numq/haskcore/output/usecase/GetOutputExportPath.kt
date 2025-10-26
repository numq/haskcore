package io.github.numq.haskcore.output.usecase

import io.github.numq.haskcore.output.OutputRepository
import io.github.numq.haskcore.usecase.UseCase

internal class GetOutputExportPath(
    private val outputRepository: OutputRepository
) : UseCase<Unit, String> {
    override suspend fun execute(input: Unit) = Result.success(outputRepository.exportPath)
}