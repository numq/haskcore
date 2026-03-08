package io.github.numq.haskcore.feature.explorer.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.explorer.core.ExplorerNode
import io.github.numq.haskcore.feature.explorer.core.ExplorerService

class ExpandDirectory(private val explorerService: ExplorerService) : UseCase<ExpandDirectory.Input, Unit> {
    data class Input(val node: ExplorerNode.Directory)

    override suspend fun Raise<Throwable>.execute(input: Input) =
        explorerService.expandDirectory(node = input.node).bind()
}