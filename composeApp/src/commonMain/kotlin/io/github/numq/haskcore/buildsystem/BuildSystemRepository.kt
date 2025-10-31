package io.github.numq.haskcore.buildsystem

import io.github.numq.haskcore.buildsystem.exception.BuildSystemException
import io.github.numq.haskcore.buildsystem.exception.UnsupportedBuildSystemException
import io.github.numq.haskcore.discovery.DiscoveryService
import io.github.numq.haskcore.stack.StackOutput
import io.github.numq.haskcore.stack.StackService
import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.Closeable
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

internal interface BuildSystemRepository : Closeable {
    val buildSystemStatus: StateFlow<BuildSystemStatus>

    val buildOutput: Flow<BuildOutput>

    suspend fun clearBuildOutput(): Result<Unit>

    suspend fun synchronize(): Result<Unit>

    suspend fun build(target: BuildSystemArtifact): Result<Flow<Unit>>

    suspend fun test(target: BuildSystemArtifact): Result<Flow<Unit>>

    suspend fun run(target: BuildSystemArtifact): Result<Flow<Unit>>

    suspend fun clean(target: BuildSystemArtifact): Result<Flow<Unit>>

    suspend fun compileFile(file: BuildSystemArtifact.HaskellFile): Result<Flow<Unit>>

    suspend fun runFile(file: BuildSystemArtifact.HaskellFile): Result<Flow<Unit>>

    suspend fun runScript(script: BuildSystemArtifact.LiterateScript): Result<Flow<Unit>>

    suspend fun getDependencies(artifact: BuildSystemArtifact): Result<List<String>>

    suspend fun getSourceFiles(artifact: BuildSystemArtifact): Result<List<BuildSystemArtifact.HaskellFile>>

    class Default(
        private val path: String, private val discoveryService: DiscoveryService, private val stackService: StackService
    ) : BuildSystemRepository {
        private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        private val _buildSystem = MutableStateFlow(BuildSystem.STACK)

        private val _buildSystemStatus = MutableStateFlow<BuildSystemStatus>(BuildSystemStatus.OutOfSync())

        override val buildSystemStatus = _buildSystemStatus.asStateFlow()

        private val _buildOutput = MutableSharedFlow<BuildOutput>(replay = 100, extraBufferCapacity = 64)

        override val buildOutput = _buildOutput.asSharedFlow()

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
            if (_buildSystemStatus.value is BuildSystemStatus.Synced) {
                _buildSystemStatus.value = BuildSystemStatus.OutOfSync()
            }
        }

        private suspend fun performSynchronization() {
            _buildSystemStatus.value = try {
                val buildProject = when {
                    stackService.isStackProject(path = path).getOrThrow() -> stackService.getProject(
                        path = path
                    ).getOrThrow().toBuildProject()

                    else -> null
                }

                val haskellFiles = discoveryService.discoverHaskellFiles(rootPath = path).getOrThrow()

                val scripts = discoveryService.discoverLiterateScripts(rootPath = path).getOrThrow()

                val artifacts = buildList {
                    if (buildProject != null) {
                        add(buildProject)
                    }

                    addAll(haskellFiles)

                    addAll(scripts)
                }

                BuildSystemStatus.Synced.Idle(system = _buildSystem.value, artifacts = artifacts)
            } catch (throwable: Throwable) {
                BuildSystemStatus.Error(throwable = throwable)
            }
        }

        private suspend fun <T : BuildSystemStatus.Synced.Active> executeOperation(
            artifact: BuildSystemArtifact,
            createStatus: (BuildSystem, List<BuildSystemArtifact>, BuildSystemArtifact) -> T,
            body: suspend () -> Result<Flow<BuildOutput>>
        ) = when (val currentStatus = _buildSystemStatus.value) {
            is BuildSystemStatus.Synced -> {
                val buildSystem = when (artifact) {
                    is BuildSystemArtifact.BuildProject -> artifact.buildSystem

                    is BuildSystemArtifact.BuildPackage -> artifact.buildSystem

                    is BuildSystemArtifact.BuildComponent -> BuildSystem.STACK

                    is BuildSystemArtifact.HaskellFile -> BuildSystem.GHC

                    is BuildSystemArtifact.LiterateScript -> BuildSystem.RUN_HASKELL
                }

                val activeStatus = createStatus(buildSystem, currentStatus.artifacts, artifact)

                _buildSystemStatus.value = activeStatus

                body().onSuccess { outputFlow ->
                    coroutineScope.launch {
                        outputFlow.collect { buildOutput ->
                            _buildOutput.emit(buildOutput)
                        }
                    }

                    _buildSystemStatus.value = BuildSystemStatus.Synced.Idle(
                        system = currentStatus.system, artifacts = currentStatus.artifacts
                    )
                }.onFailure { throwable ->
                    _buildSystemStatus.value = BuildSystemStatus.Error(throwable = throwable)
                }.map { flow -> flow.map { Unit } }
            }

            else -> Result.failure(BuildSystemException("System not in synced state"))
        }

        private suspend fun executeStackOperation(
            artifact: BuildSystemArtifact,
            operation: BuildOperation,
            stackCommand: suspend (String) -> Result<Flow<StackOutput>>
        ): Result<Flow<Unit>> {
            val path = when (artifact) {
                is BuildSystemArtifact.BuildProject.Stack -> artifact.path

                is BuildSystemArtifact.BuildPackage -> artifact.path

                else -> return Result.failure(BuildSystemException("Cannot execute stack operation on $artifact"))
            }

            return executeOperation(artifact = artifact, createStatus = { system, artifacts, currentArtifact ->
                when (operation) {
                    BuildOperation.BUILD -> BuildSystemStatus.Synced.Active.Building(
                        system = system, artifacts = artifacts, currentArtifact = currentArtifact
                    )

                    BuildOperation.TEST -> BuildSystemStatus.Synced.Active.Testing(
                        system = system, artifacts = artifacts, currentArtifact = currentArtifact
                    )

                    BuildOperation.RUN -> BuildSystemStatus.Synced.Active.Running(
                        system = system, artifacts = artifacts, currentArtifact = currentArtifact
                    )

                    BuildOperation.CLEAN -> BuildSystemStatus.Synced.Active.Building(
                        system = system, artifacts = artifacts, currentArtifact = currentArtifact
                    )

                    else -> throw BuildSystemException("Unsupported operation: $operation")
                }
            }) {
                stackCommand(path).map { flow -> flow.map { stackOutput -> stackOutput.toBuildOutput() } }
            }
        }

        init {
            coroutineScope.launch {
                startFileWatching()
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        override suspend fun clearBuildOutput() = runCatching {
            _buildOutput.resetReplayCache()
        }

        override suspend fun synchronize() = runCatching {
            when (_buildSystemStatus.value) {
                is BuildSystemStatus.OutOfSync, is BuildSystemStatus.Synced, is BuildSystemStatus.Error -> {
                    _buildSystemStatus.value = BuildSystemStatus.Syncing()

                    performSynchronization()
                }

                else -> return@runCatching
            }
        }

        override suspend fun build(target: BuildSystemArtifact) = when (target) {
            is BuildSystemArtifact.BuildProject.Stack -> executeStackOperation(
                artifact = target, operation = BuildOperation.BUILD
            ) { path ->
                stackService.build(path)
            }

            is BuildSystemArtifact.BuildPackage -> executeStackOperation(
                artifact = target, operation = BuildOperation.BUILD
            ) { path ->
                stackService.build(path)
            }

            else -> Result.failure(BuildSystemException("Cannot build $target"))
        }

        override suspend fun test(target: BuildSystemArtifact) = when (target) {
            is BuildSystemArtifact.BuildProject.Stack -> executeStackOperation(
                artifact = target, operation = BuildOperation.TEST
            ) { path ->
                stackService.test(path)
            }

            is BuildSystemArtifact.BuildPackage -> executeStackOperation(
                artifact = target, operation = BuildOperation.TEST
            ) { path ->
                stackService.test(path)
            }

            else -> Result.failure(BuildSystemException("Cannot test $target"))
        }

        override suspend fun run(target: BuildSystemArtifact) = when (target) {
            is BuildSystemArtifact.BuildProject.Stack -> executeStackOperation(
                artifact = target, operation = BuildOperation.RUN
            ) { path ->
                stackService.run(path)
            }

            is BuildSystemArtifact.BuildPackage -> executeStackOperation(
                artifact = target, operation = BuildOperation.RUN
            ) { path ->
                stackService.run(path)
            }

            is BuildSystemArtifact.HaskellFile -> runFile(file = target)

            is BuildSystemArtifact.LiterateScript -> runScript(script = target)

            else -> Result.failure(BuildSystemException("Cannot run $target"))
        }

        override suspend fun clean(target: BuildSystemArtifact) = when (target) {
            is BuildSystemArtifact.BuildProject.Stack -> executeStackOperation(
                artifact = target, operation = BuildOperation.CLEAN
            ) { path ->
                stackService.clean(path)
            }

            is BuildSystemArtifact.BuildPackage -> executeStackOperation(
                artifact = target, operation = BuildOperation.CLEAN
            ) { path ->
                stackService.clean(path)
            }

            else -> Result.failure(BuildSystemException("Cannot clean $target"))
        }

        override suspend fun compileFile(file: BuildSystemArtifact.HaskellFile) = executeOperation(
            artifact = file, createStatus = { system, artifacts, currentArtifact ->
                BuildSystemStatus.Synced.Active.Compiling(
                    system = system, artifacts = artifacts, currentArtifact = currentArtifact
                )
            }) {
            // TODO: Implement GHC compilation
            Result.failure(UnsupportedBuildSystemException)
        }

        override suspend fun runFile(file: BuildSystemArtifact.HaskellFile) = executeOperation(
            artifact = file, createStatus = { system, artifacts, currentArtifact ->
                BuildSystemStatus.Synced.Active.Running(
                    system = system, artifacts = artifacts, currentArtifact = currentArtifact
                )
            }) {
            // TODO: Implement runghc execution
            Result.failure(UnsupportedBuildSystemException)
        }

        override suspend fun runScript(script: BuildSystemArtifact.LiterateScript) = executeOperation(
            artifact = script, createStatus = { system, artifacts, currentArtifact ->
                BuildSystemStatus.Synced.Active.Running(
                    system = system,
                    artifacts = artifacts,
                    currentArtifact = currentArtifact,
                )
            }) {
            // TODO: Implement runghc for literate scripts
            Result.failure(UnsupportedBuildSystemException)
        }

        override suspend fun getDependencies(artifact: BuildSystemArtifact): Result<List<String>> = runCatching {
            when (artifact) {
                is BuildSystemArtifact.BuildProject.Stack -> {
                    // TODO: Extract dependencies from stack.yaml
                    emptyList()
                }

                is BuildSystemArtifact.BuildPackage -> {
                    // TODO: Extract dependencies from .cabal file
                    emptyList()
                }

                else -> emptyList()
            }
        }

        override suspend fun getSourceFiles(artifact: BuildSystemArtifact) = runCatching {
            when (artifact) {
                is BuildSystemArtifact.BuildComponent -> discoveryService.discoverHaskellFiles(
                    rootPath = artifact.path
                ).getOrThrow()

                is BuildSystemArtifact.BuildPackage -> artifact.components.flatMap { component ->
                    discoveryService.discoverHaskellFiles(rootPath = component.path).getOrThrow()
                }

                is BuildSystemArtifact.BuildProject -> artifact.packages.flatMap { pkg ->
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