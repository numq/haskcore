package io.github.numq.haskcore.explorer.usecase

import io.github.numq.haskcore.explorer.ExplorerNode
import io.github.numq.haskcore.explorer.ExplorerRepository
import io.github.numq.haskcore.usecase.UseCase

internal class CopyExplorerNodes(private val explorerRepository: ExplorerRepository) : UseCase<CopyExplorerNodes.Input, Unit> {
    data class Input(val nodes: Set<ExplorerNode>)

    override suspend fun execute(input: Input) = explorerRepository.copyNodes(nodes = input.nodes)
}