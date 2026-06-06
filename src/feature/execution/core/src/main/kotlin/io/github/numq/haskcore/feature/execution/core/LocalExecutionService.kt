package io.github.numq.haskcore.feature.execution.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File

internal class LocalExecutionService(
    private val scope: CoroutineScope, private val executionDataSource: ExecutionDataSource,
) : ExecutionService {
    override val fileSeparator: String = File.separator

    override val configurations = executionDataSource.executionData.map { data ->
        data.configurations.map(ExecutionConfigurationData::toExecutionConfiguration)
    }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    override val selectedConfiguration = executionDataSource.executionData.map { data ->
        data.configurations.find { configurationData ->
            configurationData.id == data.selectedConfigurationId
        }?.toExecutionConfiguration()
    }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = null)

    override suspend fun setConfigurations(
        configurations: List<ExecutionConfiguration>,
    ) = executionDataSource.update { data ->
        data.copy(configurations = configurations.map(ExecutionConfiguration::toExecutionConfigurationData))
    }.map {}

    override suspend fun updateConfiguration(
        configuration: ExecutionConfiguration,
    ) = executionDataSource.update { data ->
        data.copy(configurations = data.configurations.map { configurationData ->
            when {
                configurationData.id == configuration.id -> configuration.toExecutionConfigurationData()

                else -> configurationData
            }
        })
    }.map {}

    override suspend fun removeConfiguration(id: String) = executionDataSource.update { data ->
        data.copy(configurations = data.configurations.filterNot { configurationData ->
            configurationData.id == id
        })
    }.map {}

    override suspend fun setCurrentConfiguration(id: String?) = executionDataSource.update { data ->
        data.copy(selectedConfigurationId = id)
    }.map {}

    override fun close() {
        scope.cancel()
    }
}