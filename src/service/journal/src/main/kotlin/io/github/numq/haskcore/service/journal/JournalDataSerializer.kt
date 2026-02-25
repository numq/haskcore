package io.github.numq.haskcore.service.journal

import androidx.datastore.core.Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
internal object JournalDataSerializer : Serializer<JournalData> {
    override val defaultValue = JournalData()

    override suspend fun readFrom(input: InputStream): JournalData {
        val bytes = input.readAllBytes()

        return when {
            bytes.isEmpty() -> defaultValue

            else -> ProtoBuf.decodeFromByteArray(bytes)
        }
    }

    override suspend fun writeTo(t: JournalData, output: OutputStream) = output.write(ProtoBuf.encodeToByteArray(t))
}