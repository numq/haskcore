package io.github.numq.haskcore.feature.output.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal sealed interface OutputSessionData {
    val id: String

    val name: String

    val configuration: String

    val lines: List<OutputLineData>

    @Serializable
    @SerialName("Active")
    data class Active(
        @ProtoNumber(1) override val id: String,
        @ProtoNumber(2) override val name: String,
        @ProtoNumber(3) override val configuration: String,
        @ProtoNumber(4) override val lines: List<OutputLineData>,
    ) : OutputSessionData

    @Serializable
    @SerialName("Completed")
    data class Completed(
        @ProtoNumber(1) override val id: String,
        @ProtoNumber(2) override val name: String,
        @ProtoNumber(3) override val configuration: String,
        @ProtoNumber(4) override val lines: List<OutputLineData>,
        @ProtoNumber(5) val exitCode: Int,
        @ProtoNumber(6) val durationNanos: Long,
    ) : OutputSessionData
}