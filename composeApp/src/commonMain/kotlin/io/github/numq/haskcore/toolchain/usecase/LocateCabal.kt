package io.github.numq.haskcore.toolchain.usecase

import io.github.numq.haskcore.toolchain.ToolchainRepository
import io.github.numq.haskcore.usecase.UseCase

internal class LocateCabal(private val toolchainRepository: ToolchainRepository) : UseCase<LocateCabal.Input, Unit> {
    data class Input(val path: String)

    override suspend fun execute(input: Input) = toolchainRepository.locateCabal(path = input.path)
}