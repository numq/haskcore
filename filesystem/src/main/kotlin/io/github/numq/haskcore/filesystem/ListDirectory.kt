package io.github.numq.haskcore.filesystem

import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.filesystem.internal.FileSystemItem
import io.github.numq.haskcore.filesystem.internal.FileSystemService

class ListDirectory(private val fileSystem: FileSystemService) : UseCase<ListDirectory.Input, List<FileSystemItem>> {
    data class Input(val path: String, val recursive: Boolean)

    override suspend fun execute(input: Input) = with(input) {
        fileSystem.listDirectory(path = path, recursive = recursive)
    }
}