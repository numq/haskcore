package io.github.numq.haskcore.service.configuration

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class ConfigurationData(@ProtoNumber(1) val timestampNanos: Long = 0L)