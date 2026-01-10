package io.github.numq.haskcore.toolchain.usecase

import io.github.numq.haskcore.toolchain.ToolchainRepository
import io.github.numq.haskcore.usecase.UseCase

internal class LocateStack(private val toolchainRepository: ToolchainRepository) : UseCase<LocateStack.Input, Unit> {
    data class Input(val path: String)

    override suspend fun execute(input: Input) = toolchainRepository.locateStack(path = input.path)
}