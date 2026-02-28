package io.github.numq.haskcore.service.configuration

import io.github.numq.haskcore.core.timestamp.Timestamp

internal fun ConfigurationData.toConfiguration() = Configuration(timestamp = Timestamp(nanoseconds = timestampNanos))