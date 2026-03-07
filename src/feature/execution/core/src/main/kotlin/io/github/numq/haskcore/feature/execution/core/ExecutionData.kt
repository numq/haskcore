package io.github.numq.haskcore.feature.execution.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class ExecutionData(@ProtoNumber(1) val selectedArtifactPath: String? = null)