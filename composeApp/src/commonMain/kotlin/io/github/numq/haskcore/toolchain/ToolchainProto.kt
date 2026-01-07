package io.github.numq.haskcore.toolchain

import kotlinx.serialization.Serializable

@Serializable
internal data class ToolchainProto(
    val cabalPath: String? = null, val ghcPath: String? = null, val stackPath: String? = null
)