package io.github.numq.haskcore.filesystem

data class FileSystemException(override val cause: Throwable?) : Exception(cause)