package io.github.numq.haskcore.buildsystem

import io.github.numq.haskcore.buildsystem.exception.BuildSystemException
import io.github.numq.haskcore.buildsystem.exception.UnsupportedBuildSystemException
import io.github.numq.haskcore.discovery.DiscoveryService
import io.github.numq.haskcore.stack.StackOutput
import io.github.numq.haskcore.stack.StackService
import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.Closeable
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

internal interface BuildSystemRepository : Closeable {
    val status: StateFlow<BuildStatus>

    suspend fun synchronize(): Result<Unit>

    suspend fun build(target: BuildTarget): Result<Flow<BuildOutput>>

    suspend fun test(target: BuildTarget): Result<Flow<BuildOutput>>

    suspend fun run(target: BuildTarget): Result<Flow<BuildOutput>>

    suspend fun clean(target: BuildTarget): Result<Flow<BuildOutput>>

    suspend fun compileFile(target: BuildTarget.HaskellFile): Result<Flow<BuildOutput>>

    suspend fun runFile(target: BuildTarget.HaskellFile): Result<Flow<BuildOutput>>

    suspend fun runScript(target: BuildTarget.LiterateScript): Result<Flow<BuildOutput>>

    suspend fun getDependencies(target: BuildTarget): Result<List<String>>

    suspend fun getSourceFiles(target: BuildTarget): Result<List<BuildTarget.HaskellFile>>

    class Default(
        private val path: String, private val discoveryService: DiscoveryService, private val stackService: StackService
    ) : BuildSystemRepository {
        private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        private val buildSystem = atomic(BuildSystem.STACK)

        private val _status = MutableStateFlow<BuildStatus>(BuildStatus.OutOfSync())

        override val status = _status.asStateFlow()

        private fun isBuildConfigFile(path: Path) = with(path.fileName.absolutePathString()) {
            equals("stack.yaml") || equals("package.yaml") || endsWith(".cabal")
        }

        private fun handleFileSystemChange(event: DirectoryChangeEvent) {
            if (isBuildConfigFile(event.path())) {
                when (event.eventType()) {
                    DirectoryChangeEvent.EventType.CREATE, DirectoryChangeEvent.EventType.MODIFY, DirectoryChangeEvent.EventType.DELETE -> markAsOutOfSync()

                    else -> return
                }
            }
        }

        private suspend fun startFileWatching() {
            var watcher: DirectoryWatcher? = null

            var future: CompletableFuture<Void>? = null

            try {
                watcher = DirectoryWatcher.builder().path(Path(path)).listener { event ->
                    handleFileSystemChange(event)
                }.build()

                future = watcher.watchAsync()

                awaitCancellation()
            } finally {
                future?.cancel(true)

                watcher?.close()
            }
        }

        private fun markAsOutOfSync() {
            if (_status.value is BuildStatus.Synced) {
                _status.value = BuildStatus.OutOfSync()
            }
        }

        private suspend fun performSynchronization() {
            _status.value = try {
                val targets = when (buildSystem.value) {
                    BuildSystem.GHC -> discoveryService.discoverHaskellFiles(rootPath = path)

                    BuildSystem.RUN_HASKELL -> discoveryService.discoverLiterateScripts(rootPath = path)

                    BuildSystem.STACK -> discoveryService.discoverStackProjects(rootPath = path)

                    BuildSystem.CABAL -> discoveryService.discoverCabalProjects(rootPath = path)
                }.getOrThrow()

                BuildStatus.Synced.Idle(system = buildSystem.value, targets = targets)
            } catch (throwable: Throwable) {
                BuildStatus.Error(throwable = throwable)
            }
        }

        private suspend fun <T : BuildStatus.Synced.Active> executeDefaultConfiguration(
            target: BuildTarget,
            createStatus: (BuildSystem, List<BuildTarget>, BuildTarget) -> T,
            body: suspend () -> Result<Flow<BuildOutput>>
        ) = when (val currentStatus = _status.value) {
            is BuildStatus.Synced -> {
                val buildSystem = when (target) {
                    is BuildTarget.BuildProject -> target.buildSystem

                    is BuildTarget.HaskellFile -> BuildSystem.GHC

                    is BuildTarget.LiterateScript -> BuildSystem.RUN_HASKELL
                }

                val activeStatus = createStatus(buildSystem, currentStatus.targets, target)

                _status.value = activeStatus

                body().onSuccess {
                    _status.value = BuildStatus.Synced.Idle(
                        system = currentStatus.system, targets = currentStatus.targets
                    )
                }.onFailure { throwable ->
                    _status.value = BuildStatus.Error(throwable = throwable)
                }
            }

            is BuildStatus.OutOfSync -> Result.failure(BuildSystemException("System out of sync"))

            is BuildStatus.Syncing -> Result.failure(BuildSystemException("System syncing"))

            is BuildStatus.Error -> Result.failure(BuildSystemException("System in error state"))
        }

        private suspend fun executeDefaultStackConfiguration(
            target: BuildTarget,
            projectOperation: BuildProjectOperation,
            stackCommand: suspend (String) -> Result<Flow<StackOutput>>
        ): Result<Flow<BuildOutput>> {
            val path = when (target) {
                is BuildTarget.BuildProject.Stack -> target.path

                else -> return Result.failure(BuildSystemException("Cannot execute stack project operation on $target"))
            }

            return executeDefaultConfiguration(
                target = target, createStatus = { system, targets, currentTarget ->
                    when (projectOperation) {
                        BuildProjectOperation.BUILD -> BuildStatus.Synced.Active.Building(
                            system = system, targets = targets, currentTarget = currentTarget
                        )

                        BuildProjectOperation.TEST -> BuildStatus.Synced.Active.Testing(
                            system = system, targets = targets, currentTarget = currentTarget
                        )

                        BuildProjectOperation.RUN -> BuildStatus.Synced.Active.Running(
                            system = system, targets = targets, currentTarget = currentTarget
                        )

                        BuildProjectOperation.CLEAN -> BuildStatus.Synced.Active.Building(
                            system = system, targets = targets, currentTarget = currentTarget
                        )
                    }
                }) {
                stackCommand(path).map { flow ->
                    flow.map { stackOutput ->
                        stackOutput.toBuildOutput(target = target)
                    }
                }
            }
        }

        override suspend fun build(target: BuildTarget) = when (target) {
            is BuildTarget.BuildProject.Stack -> executeDefaultStackConfiguration(
                target = target, projectOperation = BuildProjectOperation.BUILD
            ) { path ->
                stackService.build(path)
            }

            else -> Result.failure(BuildSystemException("Cannot build ${target.name}"))
        }

        override suspend fun test(target: BuildTarget) = when (target) {
            is BuildTarget.BuildProject.Stack -> executeDefaultStackConfiguration(
                target = target, projectOperation = BuildProjectOperation.TEST
            ) { path ->
                stackService.test(path)
            }

            else -> Result.failure(BuildSystemException("Cannot test ${target.name}"))
        }

        override suspend fun run(target: BuildTarget) = when (target) {
            is BuildTarget.BuildProject.Stack -> executeDefaultStackConfiguration(
                target = target, projectOperation = BuildProjectOperation.RUN
            ) { path ->
                stackService.run(path)
            }

            is BuildTarget.HaskellFile -> runFile(target = target)

            is BuildTarget.LiterateScript -> runScript(target = target)

            else -> Result.failure(BuildSystemException("Cannot run ${target.name}"))
        }

        override suspend fun clean(target: BuildTarget) = when (target) {
            is BuildTarget.BuildProject.Stack -> executeDefaultStackConfiguration(
                target = target, projectOperation = BuildProjectOperation.CLEAN
            ) { path ->
                stackService.clean(path)
            }

            else -> Result.failure(BuildSystemException("Cannot clean ${target.name}"))
        }

        override suspend fun compileFile(target: BuildTarget.HaskellFile) = executeDefaultConfiguration(
            target = target, createStatus = { system, targets, currentTarget ->
                BuildStatus.Synced.Active.Compiling(
                    system = system, targets = targets, currentTarget = currentTarget
                )
            }) {
            // TODO: Implement GHC compilation
            Result.failure(UnsupportedBuildSystemException)
        }

        override suspend fun runFile(target: BuildTarget.HaskellFile) = executeDefaultConfiguration(
            target = target, createStatus = { system, targets, currentTarget ->
                BuildStatus.Synced.Active.Running(
                    system = system, targets = targets, currentTarget = currentTarget
                )
            }) {
            // TODO: Implement runghc execution
            Result.failure(UnsupportedBuildSystemException)
        }

        override suspend fun runScript(target: BuildTarget.LiterateScript) = executeDefaultConfiguration(
            target = target, createStatus = { system, targets, currentTarget ->
                BuildStatus.Synced.Active.Running(
                    system = system,
                    targets = targets,
                    currentTarget = currentTarget,
                )
            }) {
            // TODO: Implement runghc for literate scripts
            Result.failure(UnsupportedBuildSystemException)
        }

        init {
            coroutineScope.launch {
                startFileWatching()
            }
        }

        override suspend fun synchronize() = runCatching {
            when (_status.value) {
                is BuildStatus.OutOfSync, is BuildStatus.Synced, is BuildStatus.Error -> {
                    _status.value = BuildStatus.Syncing()

                    performSynchronization()
                }

                else -> return@runCatching
            }
        }

        override suspend fun getDependencies(target: BuildTarget) = runCatching {
            when (target) {
                is BuildTarget.BuildProject.Stack -> {
                    // TODO: Extract dependencies from stack.yaml
                    emptyList()
                }

                else -> emptyList<String>()
            }
        }

        override suspend fun getSourceFiles(target: BuildTarget) = runCatching {
            when (target) {
                is BuildTarget.BuildProject -> target.packages.flatMap { pkg ->
                    pkg.components.flatMap { component ->
                        discoveryService.discoverHaskellFiles(rootPath = component.path).getOrThrow()
                    }
                }

                else -> emptyList()
            }
        }

        override fun close() = coroutineScope.cancel()
    }
}