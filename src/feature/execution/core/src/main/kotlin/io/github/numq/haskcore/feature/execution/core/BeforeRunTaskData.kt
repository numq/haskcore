package io.github.numq.haskcore.feature.execution.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal sealed interface BeforeRunTaskData {
    val id: String

    val isEnabled: Boolean

    @Serializable
    @SerialName("Build")
    data class Build(
        @ProtoNumber(1) override val id: String,
        @ProtoNumber(2) override val isEnabled: Boolean,
        @ProtoNumber(3) val cleanFirst: Boolean,
    ) : BeforeRunTaskData

    @Serializable
    @SerialName("ExternalTool")
    data class ExternalTool(
        @ProtoNumber(1) override val id: String,
        @ProtoNumber(2) override val isEnabled: Boolean,
        @ProtoNumber(3) val command: String,
        @ProtoNumber(4) val arguments: List<String>,
        @ProtoNumber(5) val workingDir: String? = null,
    ) : BeforeRunTaskData
}