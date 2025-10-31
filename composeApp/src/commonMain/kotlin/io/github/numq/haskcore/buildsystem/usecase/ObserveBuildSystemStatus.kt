package io.github.numq.haskcore.buildsystem.usecase

import io.github.numq.haskcore.buildsystem.BuildSystemRepository
import io.github.numq.haskcore.buildsystem.BuildSystemStatus
import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow

internal class ObserveBuildSystemStatus(
    private val buildSystemRepository: BuildSystemRepository,
) : UseCase<Unit, Flow<BuildSystemStatus>> {
    override suspend fun execute(input: Unit) = Result.success(buildSystemRepository.buildSystemStatus)
}