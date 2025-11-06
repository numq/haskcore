package io.github.numq.haskcore.command

import io.github.numq.haskcore.output.OutputMessage
import kotlinx.coroutines.flow.Flow

internal data class ExecutedCommand(val command: String, val messages: Flow<OutputMessage>)