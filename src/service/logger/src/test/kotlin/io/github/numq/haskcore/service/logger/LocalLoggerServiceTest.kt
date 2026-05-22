package io.github.numq.haskcore.service.logger

import arrow.core.Either
import io.github.numq.haskcore.common.core.log.Log
import io.github.numq.haskcore.common.core.timestamp.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// todo

@OptIn(ExperimentalCoroutinesApi::class)
internal class LocalLoggerServiceTest {
    private val loggerDataSource = mockk<LoggerDataSource>()
    private val loggerDataFlow = MutableStateFlow(emptyList<LoggerData>())
    private val formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault())
        .withLocale(Locale.getDefault())

    @BeforeEach
    fun setUp() {
        every { loggerDataSource.loggerData } returns loggerDataFlow
    }

    private fun TestScope.createService(projectId: String? = null) = LocalLoggerService(
        projectId = projectId,
        scope = this,
        internalDateTimeFormatter = formatter,
        externalDateTimeFormatter = formatter,
        labelDateTimeFormatter = formatter,
        loggerDataSource = loggerDataSource
    )

    @Test
    fun `should map info log correctly`() = runTest {
        val service = createService()
        val timestamp = 1_700_000_000_000_000_000L
        val dataLog = LoggerData.Info(null, "Test info", timestamp)

        loggerDataFlow.value = listOf(dataLog)
        advanceUntilIdle()

        val logs = service.logs.first { it.isNotEmpty() }
        assertEquals(1, logs.size)
        val log = logs.first()
        assertTrue(log is Log.Info)
        assertEquals("Test info", log.message)
        assertEquals(timestamp, log.timestamp.nanoseconds)
    }

    @Test
    fun `should map warning log correctly`() = runTest {
        val service = createService()
        val timestamp = 1_700_000_000_000_000_000L
        val dataLog = LoggerData.Warning("project-1", "Test warning", timestamp)

        loggerDataFlow.value = listOf(dataLog)
        advanceUntilIdle()

        val logs = service.logs.first { it.isNotEmpty() }
        assertEquals(1, logs.size)
        val log = logs.first()
        assertTrue(log is Log.Warning)
        assertEquals("Test warning", log.message)
        assertEquals(timestamp, log.timestamp.nanoseconds)
    }

    @Test
    fun `should map handled error log correctly`() = runTest {
        val service = createService()
        val timestamp = 1_700_000_000_000_000_000L
        val dataLog = LoggerData.Error.Handled(
            projectId = null,
            message = "Test error",
            timestampNanos = timestamp,
            className = "TestException",
            stackTrace = "stack trace"
        )

        loggerDataFlow.value = listOf(dataLog)
        advanceUntilIdle()

        val logs = service.logs.first { it.isNotEmpty() }
        assertEquals(1, logs.size)
        val log = logs.first()
        assertTrue(log is Log.Error.Handled)
        assertEquals("Test error", log.message)
        assertEquals(timestamp, log.timestamp.nanoseconds)
        assertEquals("TestException", log.className)
        assertEquals("stack trace", log.stackTrace)
    }

    @Test
    fun `should map internal error log correctly`() = runTest {
        val service = createService()
        val timestamp = 1_700_000_000_000_000_000L
        val dataLog = LoggerData.Error.Internal(
            projectId = "project-1",
            message = "Internal error",
            timestampNanos = timestamp,
            className = "InternalException",
            stackTrace = "internal stack trace"
        )

        loggerDataFlow.value = listOf(dataLog)
        advanceUntilIdle()

        val logs = service.logs.first { it.isNotEmpty() }
        assertEquals(1, logs.size)
        val log = logs.first()
        assertTrue(log is Log.Error.Internal)
        assertEquals("Internal error", log.message)
        assertEquals(timestamp, log.timestamp.nanoseconds)
        assertEquals("InternalException", log.className)
        assertEquals("internal stack trace", log.stackTrace)
    }

    @Test
    fun `should map critical error log correctly`() = runTest {
        val service = createService()
        val timestamp = 1_700_000_000_000_000_000L
        val dataLog = LoggerData.Error.Critical(
            projectId = null,
            message = "Critical error",
            timestampNanos = timestamp,
            className = "CriticalException",
            stackTrace = "critical stack trace"
        )

        loggerDataFlow.value = listOf(dataLog)
        advanceUntilIdle()

        val logs = service.logs.first { it.isNotEmpty() }
        assertEquals(1, logs.size)
        val log = logs.first()
        assertTrue(log is Log.Error.Critical)
        assertEquals("Critical error", log.message)
        assertEquals(timestamp, log.timestamp.nanoseconds)
        assertEquals("CriticalException", log.className)
        assertEquals("critical stack trace", log.stackTrace)
    }

    @Test
    fun `should filter logs by project id`() = runTest {
        val service = createService(projectId = "project-1")
        val timestamp = 1_700_000_000_000_000_000L
        val dataLog1 = LoggerData.Info("project-1", "Message 1", timestamp)
        val dataLog2 = LoggerData.Info("project-2", "Message 2", timestamp)
        val dataLog3 = LoggerData.Info(null, "Message 3", timestamp)

        loggerDataFlow.value = listOf(dataLog1, dataLog2, dataLog3)
        advanceUntilIdle()

        val logs = service.logs.first { it.isNotEmpty() }
        assertEquals(2, logs.size)
        assertTrue(logs.any { it.message == "Message 1" })
        assertTrue(logs.any { it.message == "Message 3" })
        assertTrue(logs.none { it.message == "Message 2" })
    }

    @Test
    fun `should not filter when project id is null`() = runTest {
        val service = createService(projectId = null)
        val timestamp = 1_700_000_000_000_000_000L
        val dataLog1 = LoggerData.Info("project-1", "Message 1", timestamp)
        val dataLog2 = LoggerData.Info("project-2", "Message 2", timestamp)
        val dataLog3 = LoggerData.Info(null, "Message 3", timestamp)

        loggerDataFlow.value = listOf(dataLog1, dataLog2, dataLog3)
        advanceUntilIdle()

        val logs = service.logs.first { it.isNotEmpty() }
        assertEquals(3, logs.size)
    }

    @Test
    fun `should handle multiple logs`() = runTest {
        val service = createService()
        val timestamp = 1_700_000_000_000_000_000L
        val dataLog1 = LoggerData.Info(null, "First", timestamp)
        val dataLog2 = LoggerData.Warning(null, "Second", timestamp)
        val dataLog3 = LoggerData.Error.Handled(
            null, "Third", timestamp, "Exception", "stack"
        )

        loggerDataFlow.value = listOf(dataLog1, dataLog2, dataLog3)
        advanceUntilIdle()

        val logs = service.logs.first { it.isNotEmpty() }
        assertEquals(3, logs.size)
        assertTrue(logs[0] is Log.Info)
        assertTrue(logs[1] is Log.Warning)
        assertTrue(logs[2] is Log.Error.Handled)
    }

    @Test
    fun `should handle empty logs`() = runTest {
        val service = createService()

        loggerDataFlow.value = emptyList()
        advanceUntilIdle()

        val logs = service.logs.value
        assertTrue(logs.isEmpty())
    }

    @Test
    fun `submit should call update on data source`() = runTest {
        val service = createService()
        val log = Log.Info(null, "New log", Timestamp(1000L), "")
        coEvery { loggerDataSource.update(any()) } returns Either.Right(emptyList())

        service.submit(log)
        advanceUntilIdle()

        coVerify(exactly = 1) { loggerDataSource.update(any()) }
    }

    @Test
    fun `clear should call update on data source`() = runTest {
        val service = createService()
        coEvery { loggerDataSource.update(any()) } returns Either.Right(emptyList())

        service.clear()
        advanceUntilIdle()

        coVerify(exactly = 1) { loggerDataSource.update(any()) }
    }

    @Test
    fun `should update logs after submit`() = runTest {
        val service = createService()
        val timestamp = 1_700_000_000_000_000_000L
        val initialLog = LoggerData.Info(null, "Initial", timestamp)
        loggerDataFlow.value = listOf(initialLog)
        advanceUntilIdle()

        val log = Log.Info(null, "New log", Timestamp(timestamp + 1000), "")
        val updatedLogs = listOf(initialLog, LoggerData.Info(null, "New log", timestamp + 1000))
        coEvery { loggerDataSource.update(any()) } returns Either.Right(updatedLogs)

        service.submit(log)
        loggerDataFlow.value = updatedLogs
        advanceUntilIdle()

        val logs = service.logs.first { it.size == 2 }
        assertEquals(2, logs.size)
        assertEquals("New log", logs.last().message)
    }

    @Test
    fun `close should not throw`() = runTest {
        val service = createService()

        assertDoesNotThrow {
            service.close()
        }
    }
}