package io.github.numq.haskcore.service.toolchain

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

val toolchainModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner { LocalBinaryResolver() } bind BinaryResolver::class

        scopedOwner { LocalProcessRunner() } bind ProcessRunner::class

        scopedOwner {
            val applicationPath = get<String>(qualifier = ScopePath.Application)

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            val dataStore = DataStoreFactory.create(
                serializer = ToolchainDataSerializer, scope = scope, produceFile = {
                    Path.of(applicationPath).also(Files::createDirectories).resolve("toolchain.pb").toFile()
                })

            LocalToolchainDataSource(scope = scope, dataStore = dataStore)
        } bind ToolchainDataSource::class

        scopedOwner {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            LocalToolchainService(
                scope = scope, binaryResolver = get(), processRunner = get(), toolchainDataSource = get()
            )
        } bind ToolchainService::class
    }
}