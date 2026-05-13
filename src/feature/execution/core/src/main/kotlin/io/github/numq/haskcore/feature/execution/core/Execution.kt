package io.github.numq.haskcore.feature.execution.core

import arrow.core.NonEmptyList

sealed interface Execution {
    data object Syncing : Execution

    sealed interface Synced : Execution {
        data object NotFound : Synced

        sealed interface Found : Synced {
            val configurations: NonEmptyList<ExecutionConfiguration>

            val currentConfiguration: ExecutionConfiguration

            data class Stopped(
                override val configurations: NonEmptyList<ExecutionConfiguration>,
                override val currentConfiguration: ExecutionConfiguration,
            ) : Found

            data class Running(
                override val configurations: NonEmptyList<ExecutionConfiguration>,
                override val currentConfiguration: ExecutionConfiguration,
            ) : Found
        }
    }

    data class Error(val throwable: Throwable) : Execution
}