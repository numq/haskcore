package io.github.numq.haskcore.feature.shelf.core

import androidx.datastore.core.DataStoreFactory
import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import io.github.numq.haskcore.feature.shelf.core.usecase.ObserveShelf
import io.github.numq.haskcore.feature.shelf.core.usecase.SaveLeftRatio
import io.github.numq.haskcore.feature.shelf.core.usecase.SaveRightRatio
import io.github.numq.haskcore.feature.shelf.core.usecase.SelectShelfTool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module
import java.nio.file.Files
import java.nio.file.Path

val shelfCoreModule = module {
    scope<ScopeQualifierType.Project> {
        scopedOwner {
            val projectPath = get<String>(qualifier = ScopeQualifier.Project)

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            val dataStore = DataStoreFactory.create(
                serializer = ShelfDataSerializer, scope = scope, produceFile = {
                    Path.of(projectPath, ".haskcore").also(Files::createDirectories).resolve("shelf.pb").toFile()
                })

            LocalShelfDataSource(scope = scope, dataStore = dataStore)
        } bind ShelfDataSource::class

        scopedOwner {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            DefaultShelfService(scope = scope, shelfDataSource = get())
        } bind ShelfService::class

        scopedOwner { ObserveShelf(shelfService = get()) }

        scopedOwner { SaveLeftRatio(shelfService = get()) }

        scopedOwner { SaveRightRatio(shelfService = get()) }

        scopedOwner { SelectShelfTool(shelfService = get()) }
    }
}