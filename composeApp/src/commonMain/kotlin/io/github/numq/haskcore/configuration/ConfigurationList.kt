package io.github.numq.haskcore.configuration

import kotlinx.serialization.Serializable

@Serializable
internal data class ConfigurationList(val configurations: List<Configuration> = emptyList())