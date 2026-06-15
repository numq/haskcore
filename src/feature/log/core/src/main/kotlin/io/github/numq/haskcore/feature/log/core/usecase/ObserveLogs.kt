package io.github.numq.haskcore.feature.log.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.log.Log
import io.github.numq.haskcore.common.core.timestamp.Timestamp
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.service.logger.LoggerService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ObserveLogs(private val loggerService: LoggerService) : UseCase.Query<Flow<List<Log>>> {
//    override suspend fun Raise<Throwable>.query() = loggerService.logs

    override suspend fun Raise<Throwable>.query() = flowOf(listOf(
        Log.Info(
            id = "1",
            projectId = "hask-ide",
            message = "Application started",
            timestamp = Timestamp(1718265425L),
            timestampLabel = "2026-06-13 09:57:05"
        ),
        Log.Warning(
            id = "2",
            projectId = "hask-ide",
            message = "High memory usage detected",
            timestamp = Timestamp(1718265500L),
            timestampLabel = "2026-06-13 09:58:20"
        ),
        Log.Error.Handled(
            id = "3",
            projectId = "hask-ide",
            message = "Failed to parse Haskell module",
            timestamp = Timestamp(1718265600L),
            timestampLabel = "2026-06-13 10:00:00",
            className = "ParserException",
            stackTrace = "at io.github.numq.haskcore.parser.HaskellParser.parse(HaskellParser.kt:42)"
        ),
        Log.Error.Critical(
            id = "4",
            projectId = null,
            message = "IDE core engine crashed",
            timestamp = Timestamp(1718265700L),
            timestampLabel = "2026-06-13 10:01:40",
            className = "NullPointerException",
            stackTrace = "at io.github.numq.haskcore.core.Engine.run(Engine.kt:128)"
        )
    ))
}