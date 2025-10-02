package io.github.numq.haskcore.filesystem

internal data class FileSystemException(override val cause: Throwable?) : Exception(cause)