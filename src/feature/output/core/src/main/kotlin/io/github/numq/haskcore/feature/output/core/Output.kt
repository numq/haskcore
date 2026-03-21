package io.github.numq.haskcore.feature.output.core

data class Output(val sessions: List<OutputSession> = emptyList(), val activeSession: OutputSession? = null)