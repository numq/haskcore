package io.github.numq.haskcore.session

import androidx.datastore.core.Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
internal object SessionSerializer : Serializer<Session> {
    override val defaultValue = Session()

    override suspend fun readFrom(input: InputStream) = ProtoBuf.decodeFromByteArray<Session>(input.readAllBytes())

    override suspend fun writeTo(t: Session, output: OutputStream) = output.write(ProtoBuf.encodeToByteArray(t))
}