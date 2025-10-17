package io.github.numq.haskcore.stack

internal data class StackException(override val message: String) : Exception(message)