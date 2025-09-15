package io.github.numq.haskcore.filesystem

import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.filesystem.internal.FileSystemService

class CheckDirectory(private val fileSystem: FileSystemService) : UseCase<CheckDirectory.Input, Boolean> {
    data class Input(val path: String)

    override suspend fun execute(input: Input) = fileSystem.isDirectory(path = input.path)
}