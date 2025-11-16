package io.github.numq.haskcore.output

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal interface OutputRepository {
    val outputs: StateFlow<List<Output>>

    suspend fun open(output: Output): Result<Unit>

    suspend fun addMessage(outputId: String, outputMessage: OutputMessage): Result<Unit>

    suspend fun close(output: Output): Result<Unit>

    class Default : OutputRepository {
        private val _outputs = MutableStateFlow(emptyList<Output>())

        override val outputs = _outputs.asStateFlow()

        override suspend fun open(output: Output) = runCatching {
            _outputs.update { outputs -> outputs + output }
        }

        override suspend fun addMessage(outputId: String, outputMessage: OutputMessage) = runCatching {
            _outputs.update { outputs ->
                outputs.map { thisOutput ->
                    when (thisOutput.id) {
                        outputId -> thisOutput.copy(outputMessages = thisOutput.outputMessages + outputMessage)

                        else -> thisOutput
                    }
                }
            }
        }

        override suspend fun close(output: Output) = runCatching {
            _outputs.update { outputs ->
                outputs.filterNot { thisOutput -> thisOutput.id == output.id }
            }
        }
    }
}