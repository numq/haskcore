package io.github.numq.haskcore.feature.execution.core

import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
internal object ExecutionDataSerializer : Serializer<ExecutionData> {
    override val defaultValue = ExecutionData()

    override suspend fun readFrom(input: InputStream): ExecutionData {
        val bytes = withContext(Dispatchers.IO) {
            input.readAllBytes()
        }

        return when {
            bytes.isEmpty() -> defaultValue

            else -> ProtoBuf.decodeFromByteArray(bytes)
        }
    }

    override suspend fun writeTo(t: ExecutionData, output: OutputStream) = withContext(Dispatchers.IO) {
        output.write(ProtoBuf.encodeToByteArray(t))
    }
}