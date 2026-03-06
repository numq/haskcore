package io.github.numq.haskcore.service.session

data class Session(val history: List<SessionRecord> = emptyList(), val active: List<SessionRecord> = emptyList())