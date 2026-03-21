package io.github.numq.haskcore.service.journal

import androidx.datastore.core.DataStoreFactory
import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module
import java.nio.file.Files
import java.nio.file.Path

val journalModule = module {
    scope<ScopeQualifierType.Document> {
        scopedOwner {
            val projectPath = get<String>(qualifier = ScopeQualifier.Project)

            val documentPath = get<String>(qualifier = ScopeQualifier.Document)

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            val dataStore = DataStoreFactory.create(
                serializer = JournalDataSerializer, scope = scope, produceFile = {
                    val fileName = "${documentPath.hashCode()}.pb"

                    Path.of(projectPath, ".haskcore", ".journal").also(Files::createDirectories).resolve(fileName)
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