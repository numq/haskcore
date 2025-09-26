package io.github.numq.haskcore.explorer.usecase

import io.github.numq.haskcore.explorer.ExplorerNode
import io.github.numq.haskcore.explorer.ExplorerRepository
import io.github.numq.haskcore.usecase.UseCase

class InitializeExplorer(
    private val explorerRepository: ExplorerRepository
) : UseCase<InitializeExplorer.Input, ExplorerNode.Directory> {
    data class Input(val rootPath: String)

    override suspend fun execute(input: Input) = explorerRepository.initialize(rootPath = input.rootPath)
}