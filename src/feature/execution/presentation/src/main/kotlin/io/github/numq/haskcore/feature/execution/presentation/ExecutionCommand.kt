package io.github.numq.haskcore.feature.execution.presentation

import io.github.numq.haskcore.feature.execution.core.Execution
import io.github.numq.haskcore.feature.execution.core.ExecutionArtifact
import kotlinx.coroutines.flow.Flow

internal sealed interface ExecutionCommand {
    enum class Key {
        OBSERVE_EXECUTION, OBSERVE_EXECUTION_SUCCESS, RUN_ARTIFACT, SELECT_ARTIFACT
    }

    data class HandleFailure(val throwable: Throwable) : ExecutionCommand

    data object ObserveExecution : ExecutionCommand {
        val key = Key.OBSERVE_EXECUTION
    }

    data class ObserveExecutionSuccess(val flow: Flow<Execution>) : ExecutionCommand {
        val key = Key.OBSERVE_EXECUTION_SUCCESS
    }

    data class UpdateExecution(val execution: Execution) : ExecutionCommand

    data object Run : ExecutionCommand {
        val key = Key.RUN_ARTIFACT
    }

    data object RunSuccess : ExecutionCommand

    data object Stop : ExecutionCommand {
        val key = Key.RUN_ARTIFACT
    }

    data object StopSuccess : ExecutionCommand

    data class SelectArtifact(val artifact: ExecutionArtifact) : ExecutionCommand {
        val key = Key.SELECT_ARTIFACT
    }

    data object SelectArtifactSuccess : ExecutionCommand
}