package io.github.numq.haskcore.explorer

import androidx.datastore.core.Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
internal object ExplorerSnapshotSerializer : Serializer<ExplorerSnapshot> {
    override val defaultValue = ExplorerSnapshot()

    override suspend fun readFrom(input: InputStream) =
        ProtoBuf.decodeFromByteArray<ExplorerSnapshot>(input.readAllBytes())

    override suspend fun writeTo(t: ExplorerSnapshot, output: OutputStream) =
        output.write(ProtoBuf.encodeToByteArray(t))
}