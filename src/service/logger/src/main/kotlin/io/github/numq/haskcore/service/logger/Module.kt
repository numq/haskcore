package io.github.numq.haskcore.service.logger

import androidx.datastore.core.DataStoreFactory
import io.github.numq.haskcore.core.di.ScopePath
import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val loggerModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner {
            val applicationPath = get<String>(qualifier = ScopePath.Application)

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            val dataStore = DataStoreFactory.create(
                serializer = LoggerDataSerializer, scope = scope, produceFile = {
                    Path.of(applicationPath, ".haskcore").also(Files::createDirectories).resolve("log.pb").toFile()
                })

            LocalLoggerDataSource(scope = scope, dataStore = dataStore)
        } bind LoggerDataSource::class
    }

    scope<ScopeQualifier.Project> {
        scopedOwner {
            val projectPath = get<String>(qualifier = ScopePath.Project)

            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            val internalPattern = "yyyy-MM-dd HH:mm:ss.SSS"

            val externalPattern = "yyyy-MM-dd_HH-mm-ss-SSS"

            val internalDateTimeFormatter =
                DateTimeFormatter.ofPattern(internalPattern).withZone(ZoneId.systemDefault())

            val externalDateTimeFormatter =
                DateTimeFormatter.ofPattern(externalPattern).withZone(ZoneId.systemDefault())

            LocalLoggerService(
                projectId = projectPath,
                scope = scope,
                internalDateTimeFormatter = internalDateTimeFormatter,
                externalDateTimeFormatter = externalDateTimeFormatter,
                loggerDataSource = get()
            )
        } bind LoggerService::class
    }
}