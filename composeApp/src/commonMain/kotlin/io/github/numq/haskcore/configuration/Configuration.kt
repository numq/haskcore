package io.github.numq.haskcore.configuration

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
internal data class Configuration(val path: String, val name: String, val command: String) {
    val id = "${UUID.randomUUID()}"
}