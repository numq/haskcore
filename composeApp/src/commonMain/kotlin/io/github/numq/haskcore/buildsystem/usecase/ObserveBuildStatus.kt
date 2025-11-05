package io.github.numq.haskcore.buildsystem.usecase

import io.github.numq.haskcore.buildsystem.BuildSystemRepository
import io.github.numq.haskcore.buildsystem.BuildStatus
import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow

internal class ObserveBuildStatus(
    private val buildSystemRepository: BuildSystemRepository,
) : UseCase<Unit, Flow<BuildStatus>> {
    override suspend fun execute(input: Unit) = Result.success(buildSystemRepository.status)
}