package io.github.numq.haskcore.service.logger

import arrow.core.Either
import io.github.numq.haskcore.core.log.Log
import io.github.numq.haskcore.core.timestamp.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
internal class LocalLoggerServiceTest {
    private val loggerDataSource = mockk<LoggerDataSource>()
    private val loggerDataFlow = MutableStateFlow(emptyList<LoggerData>())

    @BeforeEach
    fun setup() {
        every { loggerDataSource.loggerData } returns loggerDataFlow
    }

    @Test
    fun `should map data logs to domain logs correctly`() = runTest {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        val service = LocalLoggerService(null, backgroundScope, formatter, formatter, loggerDataSource)
        val timestamp = 123456789L
        val dataLog = LoggerData.Info(null, "Test message", timestamp)

        loggerDataFlow.value = listOf(dataLog)

        advanceUntilIdle()

        val log = service.logs.drop(1).first().first()
        assert(log is Log.Info)
        assert(log.message == "Test message")
        assert(log.timestamp.nanoseconds == timestamp)
    }

    @Test
    fun `submit should call update on data source`() = runTest {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        val service = LocalLoggerService(null, backgroundScope, formatter, formatter, loggerDataSource)
        val log = Log.Info(null, "New log", Timestamp(0))
        coEvery { loggerDataSource.update(any()) } returns Either.Right(emptyList())

        service.submit(log)

        coVerify(exactly = 1) { loggerDataSource.update(any()) }
    }
}