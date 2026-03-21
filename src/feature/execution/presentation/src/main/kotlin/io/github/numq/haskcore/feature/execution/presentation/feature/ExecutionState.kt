package io.github.numq.haskcore.feature.execution.presentation.feature

import io.github.numq.haskcore.feature.execution.core.Execution

internal data class ExecutionState(val execution: Execution = Execution.Syncing)