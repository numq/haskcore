package io.github.numq.haskcore.filesystem.exception

data class FileSystemException(override val message: String) : Exception(message = message)