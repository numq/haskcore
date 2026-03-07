package io.github.numq.haskcore.feature.execution.core

import io.github.numq.haskcore.service.runtime.RuntimeEvent
import io.github.numq.haskcore.service.runtime.RuntimeRequest
import kotlinx.coroutines.flow.Flow

internal data class ExecutionSession(val id: String, val request: RuntimeRequest, val events: Flow<RuntimeEvent>)