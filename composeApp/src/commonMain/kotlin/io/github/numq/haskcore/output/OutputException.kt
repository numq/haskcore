package io.github.numq.haskcore.output

internal data class OutputException(override val message: String) : Exception(message)