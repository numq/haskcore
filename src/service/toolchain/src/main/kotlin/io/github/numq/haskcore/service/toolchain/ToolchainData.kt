package io.github.numq.haskcore.service.toolchain

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class ToolchainData(
    @ProtoNumber(1) val ghcPath: String? = null,
    @ProtoNumber(2) val stackPath: String? = null,
    @ProtoNumber(3) val cabalPath: String? = null,
    @ProtoNumber(4) val hlsPath: String? = null
)