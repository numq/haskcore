package io.github.numq.haskcore.feature.execution.core.usecase

import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.toNonEmptyListOrNull
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.execution.core.*
import io.github.numq.haskcore.service.document.DocumentService
import io.github.numq.haskcore.service.runtime.RuntimeEvent
import io.github.numq.haskcore.service.runtime.RuntimeService
import io.github.numq.haskcore.service.toolchain.ToolchainService
import io.github.numq.haskcore.service.vfs.VfsService
import io.github.numq.haskcore.service.vfs.VirtualFile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds

class ObserveExecution(
    private val rootPath: String,
    private val documentService: DocumentService,
    private val executionService: ExecutionService,
    private val runtimeService: RuntimeService,
    private val toolchainService: ToolchainService,
    private val vfsService: VfsService,
) : UseCase.Query<Flow<Execution>> {
    private companion object {
        const val DEBOUNCE_MILLIS = 300L
    }

    private suspend fun findAvailableTargets(allFiles: List<VirtualFile>): List<LaunchTarget> = coroutineScope {
        val isStack = allFiles.any { file -> file.name.equals("stack.yaml", ignoreCase = true) }

        val packageYaml = allFiles.find { file -> file.name.equals("package.yaml", ignoreCase = true) }

        if (packageYaml != null) {
            val packageTargets = documentService.readDocument(path = packageYaml.path).map { doc ->
                var inExecutables = false

                val found = mutableListOf<LaunchTarget>()

                doc.content.lineSequence().forEach { line ->
                    val trimmed = line.trimEnd()

                    if (trimmed.trim() == "executables:") {
                        inExecutables = true
                    } else if (inExecutables) {
                        if (line.isNotEmpty() && !line.startsWith(" ") && !line.startsWith("\t") && !line.trim()
                                .startsWith("#")
                        ) {
                            inExecutables = false
                        } else if (trimmed.endsWith(":") && (line.startsWith("  ") || line.startsWith("\t")) && !line.startsWith(
                                "    "
                            )
                        ) {
                            val name = trimmed.trim().removeSuffix(":")

                            if (name.isNotEmpty()) {
                                found.add(
                                    when {
                                        isStack -> LaunchTarget.Stack(
                                            name = name, workingDir = rootPath, componentName = name
                                        )

                                        else -> LaunchTarget.Cabal(
                                            name = name, workingDir = rootPath, componentName = name
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                found
            }.getOrElse { emptyList() }

            if (packageTargets.isNotEmpty()) return@coroutineScope packageTargets
        }

        val cabalFile = allFiles.find { file -> file.extension?.lowercase() == "cabal" }

        if (cabalFile != null) {
            val cabalTargets = documentService.readDocument(path = cabalFile.path).map { document ->
                document.content.lineSequence().map(CharSequence::trim).filter { line ->
                    line.startsWith("executable", ignoreCase = true)
                }.map { line ->
                    line.toString().substringAfter("executable").trim()
                }.filter(String::isNotEmpty).map { name ->
                    when {
                        isStack -> LaunchTarget.Stack(name = name, workingDir = rootPath, componentName = name)

                        else -> LaunchTarget.Cabal(name = name, workingDir = rootPath, componentName = name)
                    }
                }.toList()
            }.getOrElse { emptyList() }

            if (cabalTargets.isNotEmpty()) return@coroutineScope cabalTargets
        }

        allFiles.filter { file ->
            file.extension?.lowercase() == "hs"
        }.map { file ->
            async {
                val isMain = documentService.readDocument(path = file.path).map { doc ->
                    doc.content.lineSequence().any { line ->
                        line.trim().startsWith("main =")
                    }
                }.getOrElse { false }

                if (isMain) {
                    LaunchTarget.File(name = file.nameWithoutExtension, workingDir = rootPath, filePath = file.path)
                } else null
            }
        }.awaitAll().filterNotNull()
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override suspend fun Raise<Throwable>.query(): Flow<Execution> {
        val relevantFilesFlow = vfsService.observeFiles(path = rootPath).bind().map { files ->
            val filtered = files.filter { file ->
                val path = file.path.lowercase()

                !path.contains(".stack-work") && !path.contains("dist-newstyle") && !path.contains(".git")
            }

            val stackYaml = filtered.find { it.name.equals("stack.yaml", ignoreCase = true) }

            val cabalFile = filtered.find { it.extension?.lowercase() == "cabal" }

            when {
                stackYaml != null -> listOfNotNull(
                    stackYaml, cabalFile, filtered.find { it.name.equals("package.yaml", ignoreCase = true) })

                cabalFile != null -> listOf(cabalFile)
                else -> filtered.filter { it.extension?.lowercase() == "hs" }
            }
        }.distinctUntilChanged { old, new ->
            old.size == new.size && old.zip(new).all { (o, n) ->
                o.path == n.path && o.lastModifiedTimestamp == n.lastModifiedTimestamp
            }
        }

        val configsFlow = relevantFilesFlow.debounce(DEBOUNCE_MILLIS.milliseconds).map { files ->
            findAvailableTargets(allFiles = files).map { target ->
                val stableId = when (target) {
                    is LaunchTarget.File -> "temp-file-${target.filePath.hashCode()}"

                    is LaunchTarget.Stack -> "temp-stack-${target.componentName.hashCode()}"

                    is LaunchTarget.Cabal -> "temp-cabal-${target.componentName.hashCode()}"
                }

                ExecutionConfiguration(
                    id = stableId,
                    name = target.name,
                    target = target,
                    programArguments = emptyList(),
                    env = emptyMap(),
                    beforeRun = listOf(BeforeRunTask.Build())
                )
            }
        }.onEach { configurations ->
            executionService.setConfigurations(configurations = configurations).bind()
        }

        val processStatusEvents =
            runtimeService.events.filter { it is RuntimeEvent.Started || it is RuntimeEvent.Terminated }
                .map { it.request.id }.onStart { emit("") }

        return configsFlow.flatMapLatest { discoveredConfigs ->
            combine(
                flow = toolchainService.toolchain,
                flow2 = executionService.configurations,
                flow3 = executionService.selectedConfiguration,
                flow4 = processStatusEvents,
                transform = { _, savedConfigs, selected, _ ->
                    savedConfigs.ifEmpty { discoveredConfigs } to selected
                }).map { (configs, selected) ->
                when (val nonEmptyConfigs = configs.toNonEmptyListOrNull()) {
                    null -> Execution.Synced.NotFound

                    else -> {
                        val currentSelected = selected ?: nonEmptyConfigs.head

                        when {
                            runtimeService.isActive(id = currentSelected.id).getOrElse {
                                false
                            } -> Execution.Synced.Found.Running(
                                configurations = nonEmptyConfigs, currentConfiguration = currentSelected
                            )

                            else -> Execution.Synced.Found.Stopped(
                                configurations = nonEmptyConfigs, currentConfiguration = currentSelected
                            )
                        }
                    }
                }
            }
        }.onStart<Execution> {
            emit(Execution.Syncing)
        }.distinctUntilChanged().catch { throwable ->
            emit(Execution.Error(throwable = throwable))
        }
    }
}