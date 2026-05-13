package io.github.numq.haskcore.feature.execution.core

data class ExecutionConfiguration(
    val id: String,
    val name: String,
    val target: LaunchTarget,
    val programArguments: List<String>,
    val env: Map<String, String>,
    val beforeRun: List<BeforeRunTask>,
)