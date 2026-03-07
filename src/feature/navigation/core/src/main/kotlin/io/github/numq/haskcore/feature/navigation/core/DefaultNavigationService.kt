package io.github.numq.haskcore.feature.navigation.core

import arrow.core.Either
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.File
import java.nio.file.Path

class DefaultNavigationService : NavigationService {
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun getInitialWorkspace(path: String) = Either.catch {
        Path.of(path, ".haskcore", "workspace.pb").toFile().takeIf(File::exists)?.readBytes()?.let { bytes ->
            ProtoBuf.decodeFromByteArray<WorkspaceData>(bytes)
        }?.toInitialWorkspace()
    }
}