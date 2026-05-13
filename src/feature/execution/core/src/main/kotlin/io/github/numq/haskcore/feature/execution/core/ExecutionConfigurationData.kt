package io.github.numq.haskcore.feature.execution.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class ExecutionConfigurationData(
    @ProtoNumber(1) val id: String,
    @ProtoNumber(2) val name: String,
    @ProtoNumber(3) val target: LaunchTargetData,
    @ProtoNumber(4) val programArguments: List<String>,
    @ProtoNumber(5) val env: Map<String, String>,
    @ProtoNumber(6) val beforeRun: List<BeforeRunTaskData>,
)