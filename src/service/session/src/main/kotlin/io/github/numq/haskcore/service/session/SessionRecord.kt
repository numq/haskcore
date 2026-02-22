package io.github.numq.haskcore.service.session

import io.github.numq.haskcore.core.timestamp.Timestamp

data class SessionRecord(val path: String, val name: String?, val timestamp: Timestamp)