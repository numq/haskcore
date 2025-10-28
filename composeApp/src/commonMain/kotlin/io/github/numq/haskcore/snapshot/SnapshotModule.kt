package io.github.numq.haskcore.snapshot

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toOkioPath
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

internal val snapshotModule = module {
    single {
        SnapshotDataSource.Default(
            dataStore = PreferenceDataStoreFactory.createWithPath(
                produceFile = {
                    val directory = when {
                        System.getProperty("os.name").contains("win", true) -> System.getenv("APPDATA")
                            ?: System.getProperty("user.home")

                        else -> System.getProperty("user.home")
                    }
                    File(directory, ".haskcore/${SnapshotPreferences.NAME}").apply {
                        parentFile?.mkdirs()
                    }.toOkioPath()
                })
        )
    } bind SnapshotDataSource::class

    single { SnapshotRepository.Default(snapshotDataSource = get()) } bind SnapshotRepository::class
}