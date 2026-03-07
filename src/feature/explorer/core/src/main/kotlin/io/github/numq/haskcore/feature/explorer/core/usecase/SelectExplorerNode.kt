package io.github.numq.haskcore.feature.explorer.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.explorer.core.ExplorerService

class SelectExplorerNode(private val explorerService: ExplorerService) : UseCase<SelectExplorerNode.Input, Unit> {
    data class Input(val path: String)

    override suspend fun Raise<Throwable>.execute(input: Input) = explorerService.selectNode(path = input.path).bind()
}