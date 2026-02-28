package io.github.numq.haskcore.service.configuration

import androidx.datastore.core.Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
internal object ConfigurationDataSerializer : Serializer<ConfigurationData> {
    override val defaultValue = ConfigurationData()

    override suspend fun readFrom(input: InputStream): ConfigurationData {
        val bytes = input.readAllBytes()

        return when {
            bytes.isEmpty() -> defaultValue

            else -> ProtoBuf.decodeFromByteArray(bytes)
        }
    }

    override suspend fun writeTo(t: ConfigurationData, output: OutputStream) =
        output.write(ProtoBuf.encodeToByteArray(t))
}