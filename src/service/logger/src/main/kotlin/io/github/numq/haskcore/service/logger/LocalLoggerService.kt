package io.github.numq.haskcore.service.logger

import arrow.core.Either
import io.github.numq.haskcore.core.log.Log
import io.github.numq.haskcore.core.timestamp.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
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
    private val labelDateTimeFormatter: DateTimeFormatter,
    private val loggerDataSource: LoggerDataSource
) : LoggerService {
    private companion object {
        const val MAX_LOGS = 100
    }

    override val logs = loggerDataSource.loggerData.map { list ->
        list.filter { loggerData ->
            loggerData.projectId == null || loggerData.projectId == projectId
        }.map { loggerData ->
            val timestamp = Timestamp(nanoseconds = loggerData.timestampNanos)

            val timestampLabel = formatTimestamp(dateTimeFormatter = labelDateTimeFormatter, timestamp = timestamp)

            loggerData.toLog(timestamp = timestamp, timestampLabel = timestampLabel)
        }
    }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    private fun formatTimestamp(dateTimeFormatter: DateTimeFormatter, timestamp: Timestamp): String {
        val nanoseconds = timestamp.nanoseconds

        val seconds = nanoseconds / 1_000_000_000

        val remainingNanos = nanoseconds % 1_000_000_000

        val temporal = Instant.ofEpochSecond(seconds, remainingNanos)

        return dateTimeFormatter.format(temporal)
    }

    override suspend fun export(path: String) = Either.catch {
        val dateTime = formatTimestamp(dateTimeFormatter = externalDateTimeFormatter, timestamp = Timestamp.now())

        val currentLogs = logs.value

        withContext(Dispatchers.IO) {
            val targetPath = Path.of(path).toAbsolutePath().normalize()

            val exportDir = when {
                Files.isRegularFile(targetPath) -> targetPath.parent ?: targetPath

                else -> targetPath
            }

            Files.createDirectories(exportDir)

            val logFile = exportDir.resolve("log-$dateTime.txt")

            logFile.bufferedWriter().use { writer ->
                currentLogs.forEach { log ->
                    val level = when (log) {
                        is Log.Info -> "INFO"

                        is Log.Warning -> "WARN"

                        is Log.Error -> "ERROR"
                    }

                    val timestamp = formatTimestamp(
                        dateTimeFormatter = internalDateTimeFormatter, timestamp = log.timestamp
                    )

                    writer.write("[$timestamp] [$level] ${log.message}\n")
                }
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