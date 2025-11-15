package io.github.numq.haskcore.buildsystem.usecase

import io.github.numq.haskcore.buildsystem.BuildSystemRepository
import io.github.numq.haskcore.usecase.UseCase

internal class SynchronizeBuildSystem(
    private val buildSystemRepository: BuildSystemRepository
) : UseCase<SynchronizeBuildSystem.Input, Unit> {
    data class Input(val path: String)

    override suspend fun execute(input: Input) = buildSystemRepository.synchronize(path = input.path)
}