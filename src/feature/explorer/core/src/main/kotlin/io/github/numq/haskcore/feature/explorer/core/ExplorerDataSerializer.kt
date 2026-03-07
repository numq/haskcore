package io.github.numq.haskcore.feature.explorer.core

import androidx.datastore.core.Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
internal object ExplorerDataSerializer : Serializer<ExplorerData> {
    override val defaultValue = ExplorerData()

    override suspend fun readFrom(input: InputStream): ExplorerData {
        val bytes = input.readAllBytes()

        return when {
            bytes.isEmpty() -> defaultValue

            else -> ProtoBuf.decodeFromByteArray(bytes)
        }
    }

    override suspend fun writeTo(t: ExplorerData, output: OutputStream) = output.write(ProtoBuf.encodeToByteArray(t))
}