package io.github.numq.haskcore.feature.log.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.service.logger.LoggerService

class ClearLogs(private val loggerService: LoggerService) : UseCase.Action {
    override suspend fun Raise<Throwable>.action() = loggerService.clear().bind()
}