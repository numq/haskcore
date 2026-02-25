package io.github.numq.haskcore.service.journal

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

val journalModule = module {
    scope<ScopeQualifier.Document> {
        scopedOwner {
            val projectPath = get<String>(qualifier = ScopePath.Project)

            val documentPath = get<String>(qualifier = ScopePath.Document)

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            val dataStore = DataStoreFactory.create(
                serializer = JournalDataSerializer, scope = scope, produceFile = {
                    val fileName = "${documentPath.hashCode()}.pb"

                    Path.of(projectPath, ".haskcore", "journal").also(Files::createDirectories).resolve(fileName)
                        .toFile()
                })

            LocalJournalDataSource(scope = scope, dataStore = dataStore)
        } bind JournalDataSource::class

        scopedOwner {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            LocalJournalService(scope = scope, journalDataSource = get())
        } bind JournalService::class
    }
}