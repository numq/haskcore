package io.github.numq.haskcore.feature.log.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.log.Log
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.service.logger.LoggerService
import kotlinx.coroutines.flow.Flow

class ObserveLogs(private val loggerService: LoggerService) : UseCase<Unit, Flow<List<Log>>> {
    override suspend fun Raise<Throwable>.execute(input: Unit) = loggerService.logs
}