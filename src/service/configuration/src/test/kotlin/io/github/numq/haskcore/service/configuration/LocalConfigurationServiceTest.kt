package io.github.numq.haskcore.service.configuration

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class LocalConfigurationServiceTest {
    private val configurationDataSource = mockk<ConfigurationDataSource>()
    private val configurationDataFlow = MutableStateFlow(ConfigurationData(timestampNanos = 1000L))

    @BeforeEach
    fun setup() {
        every { configurationDataSource.configurationData } returns configurationDataFlow
    }

    @Test
    fun `configuration flow should provide initial value`() = runTest(UnconfinedTestDispatcher()) {
        val service = LocalConfigurationService(backgroundScope, configurationDataSource)

        assertEquals(1000L, service.configuration.value.timestamp.nanoseconds)
    }

    @Test
    fun `configuration should update when data source emits new values`() = runTest(UnconfinedTestDispatcher()) {
        val newTimestamp = 5000L
        val results = mutableListOf<Configuration>()
        val service = LocalConfigurationService(backgroundScope, configurationDataSource)

        val job = launch {
            service.configuration.collect { results.add(it) }
        }

        configurationDataFlow.emit(ConfigurationData(timestampNanos = newTimestamp))

        val finalResults = results.filter { it.timestamp.nanoseconds != 0L }

        assertEquals(2, finalResults.size)
        assertEquals(1000L, finalResults[0].timestamp.nanoseconds)
        assertEquals(newTimestamp, finalResults[1].timestamp.nanoseconds)

        job.cancel()
    }
}