package io.github.numq.haskcore.feature.execution.core

sealed interface BeforeRunTask {
    val id: String

    val isEnabled: Boolean

    data class Build(
        override val id: String = "build-task", override val isEnabled: Boolean = true, val cleanFirst: Boolean = false
    ) : BeforeRunTask

    data class ExternalTool(
        override val id: String,
        override val isEnabled: Boolean = true,
        val command: String,
        val arguments: List<String>
    ) : BeforeRunTask
}