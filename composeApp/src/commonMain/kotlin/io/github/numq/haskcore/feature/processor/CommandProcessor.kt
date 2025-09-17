package io.github.numq.haskcore.feature.processor

internal interface CommandProcessor<Command> {
    val activeOperations: Int

    var onFailure: (suspend (Throwable) -> Unit)?

    suspend fun process(action: CommandProcessorAction<Command>)

    fun close()
}