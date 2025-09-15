package io.github.numq.haskcore.filesystem

import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.filesystem.internal.FileSystemService

class Move(private val fileSystem: FileSystemService) : UseCase<Move.Input, Unit> {
    data class Input(val fromPath: String, val toPath: String, val overwrite: Boolean)

    override suspend fun execute(input: Input) = with(input) {
        fileSystem.move(fromPath = fromPath, toPath = toPath, overwrite = overwrite)
    }
}
