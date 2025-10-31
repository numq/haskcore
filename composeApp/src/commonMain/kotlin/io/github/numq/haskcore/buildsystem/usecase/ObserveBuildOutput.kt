package io.github.numq.haskcore.buildsystem.usecase

import io.github.numq.haskcore.buildsystem.BuildOutput
import io.github.numq.haskcore.buildsystem.BuildSystemRepository
import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow

internal class ObserveBuildOutput(
    private val buildSystemRepository: BuildSystemRepository,
) : UseCase<Unit, Flow<BuildOutput>> {
    override suspend fun execute(input: Unit) = Result.success(buildSystemRepository.buildOutput)
}