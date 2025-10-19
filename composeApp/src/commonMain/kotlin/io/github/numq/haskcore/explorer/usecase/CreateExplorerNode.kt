package io.github.numq.haskcore.explorer.usecase

import io.github.numq.haskcore.explorer.ExplorerNode
import io.github.numq.haskcore.explorer.ExplorerRepository
import io.github.numq.haskcore.usecase.UseCase

internal class CreateExplorerNode(private val explorerRepository: ExplorerRepository) : UseCase<CreateExplorerNode.Input, Unit> {
    sealed interface Input {
        val destination: ExplorerNode

        val name: String

        data class File(override val destination: ExplorerNode, override val name: String) : Input

        data class Directory(override val destination: ExplorerNode, override val name: String) : Input
    }

    override suspend fun execute(input: Input) = with(input) {
        val validName = name.trim()

        when (this) {
            is Input.File -> explorerRepository.createFile(destination = destination, name = validName)

            is Input.Directory -> explorerRepository.createDirectory(destination = destination, name = validName)
        }
    }
}