package io.github.numq.haskcore.buildsystem

import io.github.numq.haskcore.buildsystem.cabal.CabalBuildSystemService
import io.github.numq.haskcore.buildsystem.cabal.CabalBuildSystemVersion
import io.github.numq.haskcore.buildsystem.exception.BuildSystemException
import io.github.numq.haskcore.buildsystem.ghc.GhcBuildSystemService
import io.github.numq.haskcore.buildsystem.ghc.GhcBuildSystemVersion
import io.github.numq.haskcore.buildsystem.runhaskell.RunHaskellBuildSystemService
import io.github.numq.haskcore.buildsystem.stack.StackBuildSystemService
import io.github.numq.haskcore.buildsystem.stack.StackBuildSystemVersion
import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.stream.consumeAsFlow
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory

internal interface BuildSystemRepository : Closeable {
    val status: StateFlow<BuildStatus>

    suspend fun synchronize(path: String): Result<Unit>

    suspend fun execute(command: String): Result<Flow<BuildOutput>>

    class Default(
        private val customBuildSystemService: BuildSystemService,
        private val cabalBuildSystemService: CabalBuildSystemService,
        private val ghcBuildSystemService: GhcBuildSystemService,
        private val runHaskellBuildSystemService: RunHaskellBuildSystemService,
        private val stackBuildSystemService: StackBuildSystemService
    ) : BuildSystemRepository {
        private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        private var watchingJob: Job? = null

        private val _status = MutableStateFlow<BuildStatus>(BuildStatus.OutOfSynchronization)

        override val status = _status.asStateFlow()

        private fun walkDirectory(path: Path) = when {
            !Files.exists(path) -> emptyFlow()

            !Files.isDirectory(path) -> emptyFlow()

            else -> {
                try {
                    Files.walk(path).consumeAsFlow()
                } catch (_: Throwable) {
                    emptyFlow()
                }
            }
        }.flowOn(Dispatchers.IO)

        private suspend fun findProjects(paths: List<Path>): List<BuildTarget> = coroutineScope {
            paths.filter(Path::isDirectory).map { dir ->
                async {
                    val path = dir.absolutePathString()

                    when {
                        cabalBuildSystemService.hasValidProject(path = path)
                            .getOrNull() == true -> BuildTarget.BuildProject.Cabal(
                            path = path, name = dir.fileName.toString()
                        )

                        stackBuildSystemService.hasValidProject(path = path)
                            .getOrNull() == true -> BuildTarget.BuildProject.Stack(
                            path = path, name = dir.fileName.toString()
                        )

                        else -> null
                    }
                }
            }.awaitAll().filterNotNull()
        }

        private suspend fun findHaskellFiles(paths: List<Path>) = paths.filter { path ->
            ghcBuildSystemService.isValidFile(path = path.absolutePathString()).getOrNull() == true
        }.map { file ->
            BuildTarget.HaskellFile(
                path = file.absolutePathString(), name = file.fileName.toString()
            )
        }

        private suspend fun findLiterateScripts(paths: List<Path>) = paths.filter { path ->
            runHaskellBuildSystemService.isValidScript(path = path.absolutePathString()).getOrNull() == true
        }.map { file ->
            BuildTarget.LiterateScript(
                path = file.absolutePathString(), name = file.fileName.toString()
            )
        }

        private suspend fun getBuildTargets(path: String): List<BuildTarget> {
            val rootPath = Path.of(path)

            if (!Files.isDirectory(rootPath)) {
                return emptyList()
            }

            return coroutineScope {
                val allPaths = walkDirectory(path = rootPath).toList().filterNot { path -> path.startsWith(".") }

                val projects = async { findProjects(allPaths) }

                val haskellFiles = async { findHaskellFiles(allPaths) }

                val literateScripts = async { findLiterateScripts(allPaths) }

                awaitAll(projects, haskellFiles, literateScripts).flatten()
            }
        }

        private suspend fun getBuildSystemVersions(): BuildSystemVersions {
            val stackVersion = StackBuildSystemVersion.fromString(
                versionString = stackBuildSystemService.getStackVersion().getOrThrow()
            ) ?: throw BuildSystemException("Could not parse Stack version")

            val cabalVersion = CabalBuildSystemVersion.fromString(
                versionString = cabalBuildSystemService.getCabalVersion().getOrThrow()
            ) ?: throw BuildSystemException("Could not parse Cabal version")

            val ghcVersion = GhcBuildSystemVersion.fromString(
                versionString = ghcBuildSystemService.getGhcVersion().getOrThrow()
            ) ?: throw BuildSystemException("Could not parse GHC version")

            return BuildSystemVersions(
                stackVersion = stackVersion, cabalVersion = cabalVersion, ghcVersion = ghcVersion
            )
        }

        private fun isBuildConfigFile(path: Path) = with(path.fileName.absolutePathString()) {
            equals("stack.yaml") || equals("package.yaml") || endsWith(".cabal")
        }

        private fun markAsOutOfSynchronization() {
            if (_status.value is BuildStatus.Synchronized) {
                _status.value = BuildStatus.OutOfSynchronization
            }
        }

        private fun handleFileSystemChange(event: DirectoryChangeEvent) {
            if (isBuildConfigFile(event.path())) {
                when (event.eventType()) {
                    DirectoryChangeEvent.EventType.CREATE, DirectoryChangeEvent.EventType.MODIFY, DirectoryChangeEvent.EventType.DELETE -> markAsOutOfSynchronization()

                    else -> return
                }
            }
        }

        private fun startDirectoryWatching(path: String) {
            val rootPath = Path.of(path)

            if (!Files.isDirectory(rootPath)) {
                throw BuildSystemException("Could not start directory watching")
            }

            watchingJob = coroutineScope.launch {
                var watcher: DirectoryWatcher? = null

                var future: CompletableFuture<Void>? = null

                try {
                    watcher = DirectoryWatcher.builder().path(Path.of(path)).listener { event ->
                        handleFileSystemChange(event)
                    }.build()

                    future = watcher.watchAsync()

                    awaitCancellation()
                } finally {
                    future?.cancel(true)

                    watcher?.close()
                }
            }
        }

        private fun stopDirectoryWatching() {
            watchingJob?.cancel()

            watchingJob = null
        }

        private suspend fun performSynchronization(path: String) {
            _status.value = try {
                BuildStatus.Synchronized(
                    path = path, targets = getBuildTargets(path = path), versions = getBuildSystemVersions()
                )
            } catch (throwable: Throwable) {
                BuildStatus.Error(throwable = throwable)
            }
        }

        init {
            coroutineScope.launch {
                _status.collect { status ->
                    when (status) {
                        is BuildStatus.Synchronized -> startDirectoryWatching(path = status.path)

                        else -> stopDirectoryWatching()
                    }
                }
            }
        }

        override suspend fun synchronize(path: String) = runCatching {
            when (_status.value) {
                is BuildStatus.OutOfSynchronization, is BuildStatus.Synchronized, is BuildStatus.Error -> {
                    _status.value = BuildStatus.Synchronizing

                    performSynchronization(path = path)
                }

                else -> return@runCatching
            }
        }

        override suspend fun execute(command: String) = when (val currentStatus = _status.value) {
            is BuildStatus.Synchronized -> BuildCommand.parse(
                path = currentStatus.path, command = command
            ).mapCatching { buildCommand ->
                when (buildCommand) {
                    is BuildCommand.Cabal -> cabalBuildSystemService.execute(command = buildCommand)

                    is BuildCommand.Stack -> stackBuildSystemService.execute(command = buildCommand)

                    is BuildCommand.Ghc -> ghcBuildSystemService.execute(command = buildCommand)

                    is BuildCommand.RunHaskell -> runHaskellBuildSystemService.execute(command = buildCommand)

                    is BuildCommand.Custom -> customBuildSystemService.executeBuildCommand(command = buildCommand)
                }.getOrThrow()
            }.onSuccess {
                _status.value = BuildStatus.Synchronized(
                    path = currentStatus.path, targets = currentStatus.targets, versions = currentStatus.versions
                )
            }.onFailure { throwable ->
                _status.value = BuildStatus.Error(throwable = throwable)
            }

            is BuildStatus.OutOfSynchronization -> Result.failure(BuildSystemException("System out of synchronization"))

            is BuildStatus.Synchronizing -> Result.failure(BuildSystemException("System synchronizing"))

            is BuildStatus.Error -> Result.failure(BuildSystemException("System in error state"))
        }

        override fun close() = coroutineScope.cancel()
    }
}