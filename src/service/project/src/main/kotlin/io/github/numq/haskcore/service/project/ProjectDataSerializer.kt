package io.github.numq.haskcore.service.project

import androidx.datastore.core.Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
internal object ProjectDataSerializer : Serializer<ProjectData> {
    override val defaultValue = ProjectData()

    override suspend fun readFrom(input: InputStream): ProjectData {
        val bytes = input.readAllBytes()

        return when {
            bytes.isEmpty() -> defaultValue

            else -> ProtoBuf.decodeFromByteArray(bytes)
        }
    }

    override suspend fun writeTo(t: ProjectData, output: OutputStream) = output.write(ProtoBuf.encodeToByteArray(t))
}