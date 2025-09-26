package io.github.numq.haskcore.explorer.usecase

import io.github.numq.haskcore.explorer.ExplorerNode
import io.github.numq.haskcore.explorer.ExplorerRepository
import io.github.numq.haskcore.usecase.UseCase

class DeleteNodes(private val explorerRepository: ExplorerRepository) : UseCase<DeleteNodes.Input, Unit> {
    data class Input(val nodes: List<ExplorerNode>)

    override suspend fun execute(input: Input) = explorerRepository.deleteNodes(nodes = input.nodes)
}