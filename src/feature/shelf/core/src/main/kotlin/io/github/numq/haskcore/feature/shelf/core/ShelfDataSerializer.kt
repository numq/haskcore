package io.github.numq.haskcore.feature.shelf.core

import androidx.datastore.core.Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
internal object ShelfDataSerializer : Serializer<ShelfData> {
    override val defaultValue = ShelfData()

    override suspend fun readFrom(input: InputStream): ShelfData {
        val bytes = input.readAllBytes()

        return when {
            bytes.isEmpty() -> defaultValue

            else -> ProtoBuf.decodeFromByteArray(bytes)
        }
    }

    override suspend fun writeTo(t: ShelfData, output: OutputStream) = output.write(ProtoBuf.encodeToByteArray(t))
}