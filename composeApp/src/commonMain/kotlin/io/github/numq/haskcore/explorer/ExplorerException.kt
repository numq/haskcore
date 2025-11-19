package io.github.numq.haskcore.explorer

internal data class ExplorerException(override val message: String) : Exception(message)