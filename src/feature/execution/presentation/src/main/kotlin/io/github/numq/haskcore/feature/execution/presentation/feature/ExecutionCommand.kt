package io.github.numq.haskcore.feature.execution.presentation.feature

import io.github.numq.haskcore.feature.execution.core.Execution
import io.github.numq.haskcore.feature.execution.core.ExecutionConfiguration
import kotlinx.coroutines.flow.Flow

internal sealed interface ExecutionCommand {
    enum class Key {
        OBSERVE_EXECUTION, OBSERVE_EXECUTION_SUCCESS, BUILD_CONFIGURATION, RUN_CONFIGURATION, STOP_CONFIGURATION, SELECT_CONFIGURATION, EDIT_CONFIGURATION, DELETE_CONFIGURATION
    }

    data class HandleFailure(val throwable: Throwable) : ExecutionCommand

    data object ObserveExecution : ExecutionCommand {
        val key = Key.OBSERVE_EXECUTION
    }

    data class ObserveExecutionSuccess(val flow: Flow<Execution>) : ExecutionCommand {
        val key = Key.OBSERVE_EXECUTION_SUCCESS
    }

    data class UpdateExecution(val execution: Execution) : ExecutionCommand

    data class BuildConfiguration(val configuration: ExecutionConfiguration) : ExecutionCommand {
        val key = Key.BUILD_CONFIGURATION
    }

    data object BuildConfigurationSuccess : ExecutionCommand

    data class RunConfiguration(val configuration: ExecutionConfiguration) : ExecutionCommand {
        val key = Key.RUN_CONFIGURATION
    }

    data object RunConfigurationSuccess : ExecutionCommand

    data class StopConfiguration(val configuration: ExecutionConfiguration) : ExecutionCommand {
        val key = Key.STOP_CONFIGURATION
    }

    data object StopConfigurationSuccess : ExecutionCommand

    data object RunCurrentConfiguration : ExecutionCommand

    data object StopCurrentConfiguration : ExecutionCommand

    data class RerunConfiguration(val configuration: ExecutionConfiguration) : ExecutionCommand

    data class SelectConfiguration(val configuration: ExecutionConfiguration) : ExecutionCommand {
        val key = Key.SELECT_CONFIGURATION
    }

    data object SelectConfigurationSuccess : ExecutionCommand
}