package io.github.numq.haskcore.filesystem

import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.filesystem.internal.FileSystemService

class CreateDirectory(private val fileSystemService: FileSystemService) : UseCase<CreateDirectory.Input, Unit> {
    data class Input(val path: String)

    override suspend fun execute(input: Input) = fileSystemService.createDirectory(path = input.path)
}