package io.github.numq.haskcore.feature.explorer.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.explorer.core.ExplorerService

class ToggleExplorerNode(private val explorerService: ExplorerService) : UseCase<ToggleExplorerNode.Input, Unit> {
    data class Input(val path: String)

    override suspend fun Raise<Throwable>.execute(input: Input) = explorerService.toggleNode(path = input.path).bind()
}