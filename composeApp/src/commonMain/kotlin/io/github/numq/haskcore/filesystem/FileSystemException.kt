package io.github.numq.haskcore.filesystem

data class FileSystemException(override val message: String) : Exception(message)