package io.github.numq.haskcore.explorer.usecase

import io.github.numq.haskcore.explorer.ExplorerNode
import io.github.numq.haskcore.explorer.ExplorerRepository
import io.github.numq.haskcore.usecase.UseCase

class RenameNode(private val explorerRepository: ExplorerRepository) : UseCase<RenameNode.Input, Unit> {
    data class Input(val node: ExplorerNode, val name: String)

    override suspend fun execute(input: Input) = with(input) {
        explorerRepository.renameNode(node = node, name = name)
    }
}