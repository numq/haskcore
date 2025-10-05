package io.github.numq.haskcore.explorer.usecase

import io.github.numq.haskcore.explorer.ExplorerNode
import io.github.numq.haskcore.explorer.ExplorerRepository
import io.github.numq.haskcore.usecase.UseCase

internal class PasteNodes(private val explorerRepository: ExplorerRepository) : UseCase<PasteNodes.Input, Unit> {
    data class Input(val destination: ExplorerNode)

    override suspend fun execute(input: Input) = explorerRepository.pasteNodes(destination = input.destination)
}