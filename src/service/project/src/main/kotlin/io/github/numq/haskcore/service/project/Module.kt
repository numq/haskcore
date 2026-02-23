package io.github.numq.haskcore.service.project

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

val projectModule = module {
    scope<ScopeQualifier.Project> {
        scopedOwner {
            val projectPath = get<String>(qualifier = ScopePath.Project)

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            val dataStore = DataStoreFactory.create(serializer = ProjectDataSerializer, scope = scope, produceFile = {
                Path.of(projectPath, ".haskcore").also(Files::createDirectories).resolve("project.pb").toFile()
            })

            LocalProjectDataSource(scope = scope, dataStore = dataStore)
        } bind ProjectDataSource::class

        scopedOwner {
            val projectPath = get<String>(qualifier = ScopePath.Project)

            LocalProjectService(path = projectPath, projectDataSource = get())
        } bind ProjectService::class
    }
}