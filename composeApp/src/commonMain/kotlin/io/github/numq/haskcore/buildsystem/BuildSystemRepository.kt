package io.github.numq.haskcore.buildsystem

import io.github.numq.haskcore.buildsystem.cabal.CabalBuildSystemService
import io.github.numq.haskcore.buildsystem.exception.BuildSystemException
import io.github.numq.haskcore.buildsystem.ghc.GhcBuildSystemService
import io.github.numq.haskcore.buildsystem.runhaskell.RunHaskellBuildSystemService
import io.github.numq.haskcore.buildsystem.stack.StackBuildSystemService
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

    suspend fun synchronize(): Result<Unit>

    suspend fun execute(command: BuildCommand): Result<Flow<BuildOutput>>

    class Default(
        private val path: String,
        private val customBuildSystemService: BuildSystemService,
        private val cabalBuildSystemService: CabalBuildSystemService,
        private val ghcBuildSystemService: GhcBuildSystemService,
        private val runHaskellBuildSystemService: RunHaskellBuildSystemService,
        private val stackBuildSystemService: StackBuildSystemService
    ) : BuildSystemRepository {
        private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
                            path = path,
                            name = dir.fileName.toString()
                        )

                        stackBuildSystemService.hasValidProject(path = path)
                            .getOrNull() == true -> BuildTarget.BuildProject.Stack(
                            path = path,
                            name = dir.fileName.toString()
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

        private suspend fun getBuildTargets(): List<BuildTarget> {
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

        private suspend fun startFileWatching() {
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

        private suspend fun performSynchronization() {
            _status.value = try {
                BuildStatus.Synchronized(targets = getBuildTargets())
            } catch (throwable: Throwable) {
                BuildStatus.Error(throwable = throwable)
            }
        }

        init {
            coroutineScope.launch {
                startFileWatching()
            }
        }

        override suspend fun synchronize() = runCatching {
            when (_status.value) {
                is BuildStatus.OutOfSynchronization, is BuildStatus.Synchronized, is BuildStatus.Error -> {
                    _status.value = BuildStatus.Synchronizing

                    performSynchronization()
                }

                else -> return@runCatching
            }
        }

        override suspend fun execute(command: BuildCommand) = when (val currentStatus = _status.value) {
            is BuildStatus.Synchronized -> when (command) {
                is BuildCommand.Cabal -> cabalBuildSystemService.execute(command = command)

                is BuildCommand.Stack -> stackBuildSystemService.execute(command = command)

                is BuildCommand.Ghc -> ghcBuildSystemService.execute(command = command)

                is BuildCommand.RunHaskell -> runHaskellBuildSystemService.execute(command = command)

                is BuildCommand.Custom -> customBuildSystemService.executeBuildCommand(command = command)
            }.onSuccess {
                _status.value = BuildStatus.Synchronized(targets = currentStatus.targets)
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