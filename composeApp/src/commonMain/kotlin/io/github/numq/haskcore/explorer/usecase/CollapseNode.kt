package io.github.numq.haskcore.explorer.usecase

import io.github.numq.haskcore.explorer.ExplorerNode
import io.github.numq.haskcore.explorer.ExplorerRepository
import io.github.numq.haskcore.usecase.UseCase

internal class CollapseNode(
    private val explorerRepository: ExplorerRepository
) : UseCase<CollapseNode.Input, Unit> {
    data class Input(val directory: ExplorerNode.Directory)

    override suspend fun execute(input: Input) = explorerRepository.collapseDirectory(directory = input.directory)
}