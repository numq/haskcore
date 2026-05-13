package io.github.numq.haskcore.service.session

import androidx.datastore.core.DataStoreFactory
import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module
import java.nio.file.Files
import java.nio.file.Path

val sessionServiceModule = module {
    scope<ScopeQualifier.Type.Application> {
        scopedOwner {
            val applicationPath = get<String>(qualifier = ScopeQualifier.Application)

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            val dataStore = DataStoreFactory.create(
                serializer = SessionDataSerializer, scope = scope, produceFile = {
                    Path.of(applicationPath, ".haskcore").also(Files::createDirectories).resolve("session.pb").toFile()
                })

            LocalSessionDataSource(scope = scope, dataStore = dataStore)
        } bind SessionDataSource::class

        scopedOwner {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            LocalSessionService(scope = scope, sessionDataSource = get())
        } bind SessionService::class
    }
}