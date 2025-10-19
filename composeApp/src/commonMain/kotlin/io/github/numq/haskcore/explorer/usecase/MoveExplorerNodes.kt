package io.github.numq.haskcore.explorer.usecase

import io.github.numq.haskcore.explorer.ExplorerNode
import io.github.numq.haskcore.explorer.ExplorerRepository
import io.github.numq.haskcore.usecase.UseCase

internal class MoveExplorerNodes(private val explorerRepository: ExplorerRepository) : UseCase<MoveExplorerNodes.Input, Unit> {
    data class Input(val nodes: Set<ExplorerNode>, val destination: ExplorerNode)

    override suspend fun execute(input: Input) = with(input) {
        explorerRepository.moveNodes(nodes = nodes, destination = destination)
    }
}
