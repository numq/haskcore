package io.github.numq.haskcore.feature.bootstrap.core

import io.github.numq.haskcore.common.core.timestamp.Timestamp

data class BootstrapItem(val path: String, val name: String?, val timestamp: Timestamp)