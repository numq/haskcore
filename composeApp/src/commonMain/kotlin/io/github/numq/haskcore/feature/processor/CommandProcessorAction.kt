package io.github.numq.haskcore.feature.processor

internal data class CommandProcessorAction<Command>(val command: Command, val block: suspend (Command) -> Unit)