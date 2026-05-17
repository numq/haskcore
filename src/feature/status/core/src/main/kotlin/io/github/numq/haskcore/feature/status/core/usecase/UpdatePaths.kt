package io.github.numq.haskcore.feature.status.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.service.toolchain.ToolchainService

class UpdatePaths(private val toolchainService: ToolchainService) : UseCase.Command<UpdatePaths.Input> {
    data class Input(val ghcPath: String?, val cabalPath: String?, val stackPath: String?, val hlsPath: String?)

    override suspend fun Raise<Throwable>.command(input: Input) = with(input) {
        toolchainService.updatePaths(
            ghcPath = ghcPath, cabalPath = cabalPath, stackPath = stackPath, hlsPath = hlsPath
        ).bind()
    }
}