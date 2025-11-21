package io.github.numq.haskcore.session

import androidx.datastore.core.DataStoreFactory
import io.github.numq.haskcore.session.usecase.GetSession
import io.github.numq.haskcore.session.usecase.ObserveSession
import org.koin.dsl.bind
import org.koin.dsl.module
import java.nio.file.Files
import java.nio.file.Path

internal val sessionModule = module {
    single {
        val globalPath = when {
            System.getProperty("os.name").contains("win", true) -> System.getenv("APPDATA")
                ?: System.getProperty("user.home")

            else -> System.getProperty("user.home")
        }

        val dataStore = DataStoreFactory.create(serializer = SessionSerializer, produceFile = {
            Path.of(globalPath, ".haskcore").also(Files::createDirectories).resolve("session.pb").toFile()
        })

        SessionDataSource.Default(dataStore = dataStore)
    } bind SessionDataSource::class

    single { SessionRepository.Default(sessionDataSource = get()) } bind SessionRepository::class

    single { GetSession(sessionRepository = get()) }

    single { ObserveSession(sessionRepository = get()) }
}