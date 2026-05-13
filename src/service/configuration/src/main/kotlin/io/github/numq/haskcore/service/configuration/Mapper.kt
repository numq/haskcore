package io.github.numq.haskcore.service.configuration

import io.github.numq.haskcore.common.core.timestamp.Timestamp

internal fun ConfigurationData.toConfiguration() = Configuration(timestamp = Timestamp(nanoseconds = timestampNanos))