package io.github.numq.haskcore.service.session

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

val sessionModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner {
            val applicationPath = get<String>(qualifier = ScopePath.Application)

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            val dataStore = DataStoreFactory.create(
                serializer = SessionDataSerializer, scope = scope, produceFile = {
                    Path.of(applicationPath).also(Files::createDirectories).resolve("session.pb").toFile()
                })

            LocalSessionDataSource(scope = scope, dataStore = dataStore)
        } bind SessionDataSource::class

        scopedOwner {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            LocalSessionService(scope = scope, sessionDataSource = get())
        } bind SessionService::class
    }
}