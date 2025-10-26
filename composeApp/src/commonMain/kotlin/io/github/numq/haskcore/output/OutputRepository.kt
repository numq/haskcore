package io.github.numq.haskcore.output

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal interface OutputRepository {
    val exportPath: String

    val messages: StateFlow<List<OutputMessage>>

    suspend fun send(message: OutputMessage): Result<Unit>

    suspend fun export(): Result<Unit>

    suspend fun clear(): Result<Unit>

    class Default(private val outputDataSource: OutputDataSource) : OutputRepository {
        override val exportPath = outputDataSource.dirName

        private val _messages = MutableStateFlow(emptyList<OutputMessage>())

        override val messages = _messages.asStateFlow()

        override suspend fun send(message: OutputMessage) = runCatching {
            _messages.update { messages -> messages + message }
        }

        override suspend fun export() = outputDataSource.writeData(
            dataPath = exportPath, data = _messages.value
        ).recoverCatching { throwable ->
            throw OutputException(throwable.message ?: "Failed to export output")
        }

        override suspend fun clear() = runCatching {
            _messages.update { emptyList() }
        }
    }
}