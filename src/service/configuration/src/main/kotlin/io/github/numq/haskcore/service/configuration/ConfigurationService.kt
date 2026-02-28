package io.github.numq.haskcore.service.configuration

import kotlinx.coroutines.flow.Flow
import java.io.Closeable

interface ConfigurationService : Closeable {
    val configuration: Flow<Configuration>
}