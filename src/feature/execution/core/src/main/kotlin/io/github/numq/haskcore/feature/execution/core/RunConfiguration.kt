package io.github.numq.haskcore.feature.execution.core

data class RunConfiguration(
    val id: String,
    val name: String,
    val target: LaunchTarget,
    val arguments: List<String>,
    val env: Map<String, String>,
    val beforeRun: List<BeforeRunTask>
)