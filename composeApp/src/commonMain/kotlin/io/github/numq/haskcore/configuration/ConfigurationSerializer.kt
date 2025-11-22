package io.github.numq.haskcore.configuration

import androidx.datastore.core.Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
internal object ConfigurationSerializer : Serializer<ConfigurationList> {
    override val defaultValue = ConfigurationList()

    override suspend fun readFrom(input: InputStream) =
        ProtoBuf.decodeFromByteArray<ConfigurationList>(input.readAllBytes())

    override suspend fun writeTo(t: ConfigurationList, output: OutputStream) =
        output.write(ProtoBuf.encodeToByteArray(t))
}