package io.github.numq.haskcore.feature.output.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal sealed interface OutputLineData {
    val id: String

    val text: String

    val timestampNanos: Long

    @Serializable
    @SerialName("System")
    data class System(
        @ProtoNumber(1) override val id: String,
        @ProtoNumber(2) override val text: String,
        @ProtoNumber(3) override val timestampNanos: Long
    ) : OutputLineData

    @Serializable
    @SerialName("Normal")
    data class Normal(
        @ProtoNumber(1) override val id: String,
        @ProtoNumber(2) override val text: String,
        @ProtoNumber(3) override val timestampNanos: Long
    ) : OutputLineData

    @Serializable
    @SerialName("error")
    data class Error(
        @ProtoNumber(1) override val id: String,
        @ProtoNumber(2) override val text: String,
        @ProtoNumber(3) override val timestampNanos: Long
    ) : OutputLineData
}