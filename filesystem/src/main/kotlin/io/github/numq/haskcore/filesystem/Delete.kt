package io.github.numq.haskcore.filesystem

import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.filesystem.internal.FileSystemService

class Delete(private val fileSystem: FileSystemService) : UseCase<Delete.Input, Unit> {
    data class Input(val path: String)

    override suspend fun execute(input: Input) = fileSystem.delete(path = input.path)
}