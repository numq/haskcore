package io.github.numq.haskcore.toolchain.usecase

import io.github.numq.haskcore.toolchain.ToolchainRepository
import io.github.numq.haskcore.usecase.UseCase

internal class LocateGhc(private val toolchainRepository: ToolchainRepository) : UseCase<LocateGhc.Input, Unit> {
    data class Input(val path: String)

    override suspend fun execute(input: Input) = toolchainRepository.locateGhc(path = input.path)
}