package io.github.numq.haskcore.feature.execution.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal sealed interface LaunchTargetData {
    @Serializable
    @SerialName("Stack")
    data class Stack(
        @ProtoNumber(1) val name: String,
        @ProtoNumber(2) val workingDir: String,
        @ProtoNumber(3) val componentName: String,
    ) : LaunchTargetData

    @Serializable
    @SerialName("Cabal")
    data class Cabal(
        @ProtoNumber(1) val name: String,
        @ProtoNumber(2) val workingDir: String,
        @ProtoNumber(3) val componentName: String,
    ) : LaunchTargetData

    @Serializable
    @SerialName("File")
    data class File(
        @ProtoNumber(1) val name: String, @ProtoNumber(2) val workingDir: String, @ProtoNumber(3) val filePath: String,
    ) : LaunchTargetData
}