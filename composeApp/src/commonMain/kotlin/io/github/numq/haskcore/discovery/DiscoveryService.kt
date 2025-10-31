package io.github.numq.haskcore.discovery

import io.github.numq.haskcore.buildsystem.BuildSystemArtifact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.stream.consumeAsFlow
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

internal interface DiscoveryService {
    suspend fun discoverHaskellFiles(rootPath: String): Result<List<BuildSystemArtifact.HaskellFile>>

    suspend fun discoverLiterateScripts(rootPath: String): Result<List<BuildSystemArtifact.LiterateScript>>

    suspend fun discoverBuildArtifacts(rootPath: String): Result<List<BuildSystemArtifact>>

    class Default : DiscoveryService {
        private fun walkDirectory(path: Path) = when {
            !Files.exists(path) -> emptyFlow()

            else -> Files.walk(path).consumeAsFlow()
        }.flowOn(Dispatchers.IO)

        override suspend fun discoverHaskellFiles(rootPath: String) = runCatching {
            walkDirectory(Path.of(rootPath)).filter { path ->
                path.extension == "hs" && path.isRegularFile()
            }.map { file ->
                BuildSystemArtifact.HaskellFile(
                    path = file.absolutePathString(), name = file.fileName.absolutePathString().removeSuffix(".hs")
                )
            }.toList()
        }

        override suspend fun discoverLiterateScripts(rootPath: String) = runCatching {
            walkDirectory(Path.of(rootPath)).filter { path ->
                path.extension == "lhs" && path.isRegularFile()
            }.map { file ->
                BuildSystemArtifact.LiterateScript(
                    path = file.absolutePathString(), name = file.fileName.absolutePathString().removeSuffix(".lhs")
                )
            }.toList()
        }

        override suspend fun discoverBuildArtifacts(rootPath: String) = runCatching {
            coroutineScope {
                val haskellFiles = async { discoverHaskellFiles(rootPath) }

                val literateScripts = async { discoverLiterateScripts(rootPath) }

                val filesResult = haskellFiles.await()

                val scriptsResult = literateScripts.await()

                filesResult.getOrThrow() + scriptsResult.getOrThrow()
            }
        }
    }
}