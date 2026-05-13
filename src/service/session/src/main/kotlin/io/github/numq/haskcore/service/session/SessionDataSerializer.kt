package io.github.numq.haskcore.service.session

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
internal object SessionDataSerializer : Serializer<SessionData> {
    override val defaultValue = SessionData()

    override suspend fun readFrom(input: InputStream): SessionData {
        val bytes = withContext(Dispatchers.IO) {
            input.readAllBytes()
        }

        return when {
            bytes.isEmpty() -> defaultValue

            else -> ProtoBuf.decodeFromByteArray(bytes)
        }
    }

    override suspend fun writeTo(t: SessionData, output: OutputStream) = withContext(Dispatchers.IO) {
        output.write(ProtoBuf.encodeToByteArray(t))
    }
}