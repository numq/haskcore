package io.github.numq.haskcore.feature.log.presentation.feature

import io.github.numq.haskcore.core.log.Log

internal data class LogState(val logs: List<Log> = emptyList())