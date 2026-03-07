package io.github.numq.haskcore.feature.execution.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class LocalExecutionService(
    private val scope: CoroutineScope, private val executionDataSource: ExecutionDataSource
) : ExecutionService {
    override val selectedArtifactPath = executionDataSource.executionData.map { executionData ->
        executionData.selectedArtifactPath
    }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = null)

    override suspend fun selectArtifact(artifact: ExecutionArtifact?) = executionDataSource.update { executionData ->
        executionData.copy(selectedArtifactPath = artifact?.target?.path)
    }.map {}

    override fun close() {
        scope.cancel()
    }
}