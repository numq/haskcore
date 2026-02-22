package io.github.numq.haskcore.service.session

data class Session(val history: Set<SessionRecord> = emptySet(), val active: Set<SessionRecord> = emptySet())