package io.github.numq.haskcore.buildsystem.usecase

import io.github.numq.haskcore.buildsystem.BuildSystemRepository
import io.github.numq.haskcore.usecase.UseCase

internal class Synchronize(
    private val buildSystemRepository: BuildSystemRepository
) : UseCase<Unit, Unit> {
    override suspend fun execute(input: Unit) = buildSystemRepository.synchronize()
}