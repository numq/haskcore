package io.github.numq.haskcore.service.vfs

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
internal object SnapshotDataSerializer : Serializer<SnapshotData?> {
    override val defaultValue = null

    override suspend fun readFrom(input: InputStream): SnapshotData? {
        val bytes = withContext(Dispatchers.IO) {
            input.readAllBytes()
        }

        return when {
            bytes.isEmpty() -> null

            else -> ProtoBuf.decodeFromByteArray(bytes)
        }
    }

    override suspend fun writeTo(t: SnapshotData?, output: OutputStream) {
        if (t != null) {
            withContext(Dispatchers.IO) {
                output.write(ProtoBuf.encodeToByteArray(t))
            }
        }
    }
}