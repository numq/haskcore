package io.github.numq.haskcore.service.logger

import arrow.core.Either
import io.github.numq.haskcore.core.log.Log
import io.github.numq.haskcore.core.timestamp.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.io.path.bufferedWriter

internal class LocalLoggerService(
    private val projectId: String?,
    private val scope: CoroutineScope,
    private val internalDateTimeFormatter: DateTimeFormatter,
    private val externalDateTimeFormatter: DateTimeFormatter,
    private val loggerDataSource: LoggerDataSource
) : LoggerService {
    private companion object {
        const val MAX_LOGS = 1_000
    }

    override val logs = loggerDataSource.loggerData.map { list ->
        list.filter { loggerData ->
            loggerData.projectId == null || loggerData.projectId == projectId
        }.map(LoggerData::toLog)
    }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    private fun formatTimestamp(dateTimeFormatter: DateTimeFormatter, timestamp: Timestamp): String {
        val nanoseconds = timestamp.nanoseconds

        val seconds = nanoseconds / 1_000_000_000

        val remainingNanos = nanoseconds % 1_000_000_000

        val temporal = Instant.ofEpochSecond(seconds, remainingNanos)

        return dateTimeFormatter.format(temporal)
    }

    override suspend fun export(path: String) = Either.catch {
        val loggerData = loggerDataSource.loggerData.first()

        val dateTime = formatTimestamp(dateTimeFormatter = externalDateTimeFormatter, timestamp = Timestamp.now())

        Path.of(path).also(Files::createDirectories).resolve("log-${dateTime}.txt").bufferedWriter().use { writer ->
            loggerData.forEach { loggerData ->
                val level = when (loggerData) {
                    is LoggerData.Info -> "INFO"

                    is LoggerData.Warning -> "WARN"

                    is LoggerData.Error -> "ERROR"
                }

                val timestamp = formatTimestamp(
                    dateTimeFormatter = internalDateTimeFormatter,
                    timestamp = Timestamp(nanoseconds = loggerData.timestampNanos)
                )

                writer.write("[$timestamp] [$level] ${loggerData.message}\n")
            }
        }
    }

    override suspend fun submit(log: Log) = loggerDataSource.update { loggerData ->
        loggerData.plus(log.toLogData()).takeLast(MAX_LOGS)
    }.map {}

    override suspend fun clear() = loggerDataSource.update { loggerData ->
        emptyList()
    }.map {}

    override fun close() {
        scope.cancel()
    }
}