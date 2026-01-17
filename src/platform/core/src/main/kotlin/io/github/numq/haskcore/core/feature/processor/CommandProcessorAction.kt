package io.github.numq.haskcore.core.feature.processor

data class CommandProcessorAction<Command>(val command: Command, val block: suspend (Command) -> Unit)