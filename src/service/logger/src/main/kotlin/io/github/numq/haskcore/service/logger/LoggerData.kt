package io.github.numq.haskcore.service.logger

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal sealed interface LoggerData {
    val projectId: String?

    val message: String

    val timestampNanos: Long

    @Serializable
    @SerialName("info")
    data class Info(
        @ProtoNumber(1) override val projectId: String?,
        @ProtoNumber(2) override val message: String,
        @ProtoNumber(3) override val timestampNanos: Long
    ) : LoggerData

    @Serializable
    @SerialName("warning")
    data class Warning(
        @ProtoNumber(1) override val projectId: String?,
        @ProtoNumber(2) override val message: String,
        @ProtoNumber(3) override val timestampNanos: Long
    ) : LoggerData

    @Serializable
    sealed interface Error : LoggerData {
        val className: String

        val stackTrace: String

        @Serializable
        @SerialName("handled")
        data class Handled(
            @ProtoNumber(1) override val projectId: String?,
            @ProtoNumber(2) override val message: String,
            @ProtoNumber(3) override val timestampNanos: Long,
            @ProtoNumber(4) override val className: String,
            @ProtoNumber(5) override val stackTrace: String
        ) : Error

        @Serializable
        @SerialName("internal")
        data class Internal(
            @ProtoNumber(1) override val projectId: String?,
            @ProtoNumber(2) override val message: String,
            @ProtoNumber(3) override val timestampNanos: Long,
            @ProtoNumber(4) override val className: String,
            @ProtoNumber(5) override val stackTrace: String
        ) : Error

        @Serializable
        @SerialName("critical")
        data class Critical(
            @ProtoNumber(1) override val projectId: String?,
            @ProtoNumber(2) override val message: String,
            @ProtoNumber(3) override val timestampNanos: Long,
            @ProtoNumber(4) override val className: String,
            @ProtoNumber(5) override val stackTrace: String
        ) : Error
    }
}