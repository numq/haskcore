package io.github.numq.haskcore.service.logger

import androidx.datastore.core.Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
internal object LoggerDataSerializer : Serializer<List<LoggerData>> {
    override val defaultValue = emptyList<LoggerData>()

    override suspend fun readFrom(input: InputStream): List<LoggerData> {
        val bytes = input.readAllBytes()

        return when {
            bytes.isEmpty() -> defaultValue

            else -> ProtoBuf.decodeFromByteArray(bytes)
        }
    }

    override suspend fun writeTo(t: List<LoggerData>, output: OutputStream) =
        output.write(ProtoBuf.encodeToByteArray(t))
}