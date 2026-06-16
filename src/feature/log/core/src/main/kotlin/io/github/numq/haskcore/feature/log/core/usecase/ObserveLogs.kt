package io.github.numq.haskcore.feature.log.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.log.Log
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.service.logger.LoggerService
import kotlinx.coroutines.flow.Flow

class ObserveLogs(private val loggerService: LoggerService) : UseCase.Query<Flow<List<Log>>> {
    override suspend fun Raise<Throwable>.query() = loggerService.logs
}