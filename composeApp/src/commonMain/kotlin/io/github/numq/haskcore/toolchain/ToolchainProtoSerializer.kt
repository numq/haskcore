package io.github.numq.haskcore.toolchain

import androidx.datastore.core.Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
internal object ToolchainProtoSerializer : Serializer<ToolchainProto> {
    override val defaultValue = ToolchainProto()

    override suspend fun readFrom(input: InputStream) =
        ProtoBuf.decodeFromByteArray<ToolchainProto>(input.readAllBytes())

    override suspend fun writeTo(t: ToolchainProto, output: OutputStream) =
        output.write(ProtoBuf.encodeToByteArray(t))
}