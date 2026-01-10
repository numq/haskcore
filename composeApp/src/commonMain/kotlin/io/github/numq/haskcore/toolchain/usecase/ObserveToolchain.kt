package io.github.numq.haskcore.toolchain.usecase

import io.github.numq.haskcore.toolchain.Toolchain
import io.github.numq.haskcore.toolchain.ToolchainRepository
import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow

internal class ObserveToolchain(private val toolchainRepository: ToolchainRepository) : UseCase<Unit, Flow<Toolchain>> {
    override suspend fun execute(input: Unit) = Result.success(toolchainRepository.toolchain)
}