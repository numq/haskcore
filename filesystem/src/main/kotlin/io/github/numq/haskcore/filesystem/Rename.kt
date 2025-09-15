package io.github.numq.haskcore.filesystem

import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.filesystem.internal.FileSystemService

class Rename(private val fileSystem: FileSystemService) : UseCase<Rename.Input, Unit> {
    data class Input(val path: String, val name: String)

    override suspend fun execute(input: Input) = with(input) {
        fileSystem.rename(path = path, name = name)
    }
}