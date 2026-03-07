package io.github.numq.haskcore.feature.execution.core

import arrow.core.NonEmptyList

sealed interface Execution {
    data object OutOfSync : Execution

    data object Syncing : Execution

    sealed interface Synced : Execution {
        data object NotFound : Synced

        sealed interface Found : Synced {
            val artifacts: List<ExecutionArtifact>

            val selectedArtifact: ExecutionArtifact

            data class Stopped(
                override val artifacts: NonEmptyList<ExecutionArtifact>,
                override val selectedArtifact: ExecutionArtifact = artifacts.head
            ) : Found {
                init {
                    require(artifacts.isNotEmpty()) { "There must be at least one artifact" }
                }
            }

            data class Running(
                override val artifacts: NonEmptyList<ExecutionArtifact>,
                override val selectedArtifact: ExecutionArtifact = artifacts.head
            ) : Found {
                init {
                    require(artifacts.isNotEmpty()) { "There must be at least one artifact" }
                }
            }
        }
    }

    data class Error(val throwable: Throwable) : Execution
}