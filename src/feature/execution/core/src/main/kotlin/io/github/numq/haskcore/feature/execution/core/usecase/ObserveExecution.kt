package io.github.numq.haskcore.feature.execution.core.usecase

import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.toNonEmptyListOrNull
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.execution.core.*
import io.github.numq.haskcore.service.document.DocumentService
import io.github.numq.haskcore.service.runtime.RuntimeEvent
import io.github.numq.haskcore.service.runtime.RuntimeRequest
import io.github.numq.haskcore.service.runtime.RuntimeService
import io.github.numq.haskcore.service.toolchain.ToolchainService
import io.github.numq.haskcore.service.vfs.VfsService
import io.github.numq.haskcore.service.vfs.VirtualFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

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

    private suspend fun findAvailableTargets(allFiles: List<VirtualFile>): List<LaunchTarget> {
        val stackYaml = allFiles.find { file -> file.name.equals("stack.yaml", ignoreCase = true) }

        if (stackYaml != null) {
            val request = RuntimeRequest.Stack(
                id = "probe-${rootPath.hashCode()}",
                name = "probe-targets",
                arguments = listOf("ide", "targets"),
                workingDir = rootPath
            )

            val events = runtimeService.execute(request = request).getOrElse { emptyFlow() }.toList()

            val targets = events.filter { event ->
                event is RuntimeEvent.Stdout || event is RuntimeEvent.Stderr
            }.flatMap { event ->
                when (event) {
                    is RuntimeEvent.Stdout -> event.text

                    is RuntimeEvent.Stderr -> event.text

                    else -> ""
                }.trim().lines()
            }.map(String::trim).filter { line ->
                line.isNotEmpty() && !line.startsWith("#") && line.contains(":")
            }.map { targetInfo ->
                val parts = targetInfo.split(":")

                val name = when {
                    parts.size >= 3 && parts[1] == "exe" -> parts[2]

                    parts.size >= 2 -> parts[1]

                    else -> parts.last()
                }

                LaunchTarget.Stack(name = name, workingDir = rootPath, componentName = targetInfo)
            }.filter { target ->
                target.name.isNotEmpty()
            }

            if (targets.isNotEmpty()) return targets
        }

        val cabalFile = allFiles.find { file -> file.extension?.lowercase() == "cabal" }

        if (cabalFile != null) {
            val cabalTargets = documentService.readDocument(path = cabalFile.path).map { document ->
                document.content.lineSequence().map(CharSequence::trim).filter { line ->
                    line.startsWith("executable", ignoreCase = true)
                }.map { line ->
                    line.toString().substringAfter("executable").trim()
                }.filter(String::isNotEmpty).map { name ->
                    LaunchTarget.Cabal(name = name, workingDir = rootPath, componentName = name)
                }.toList()
            }.getOrElse { emptyList() }

            if (cabalTargets.isNotEmpty()) return cabalTargets
        }

        return allFiles.filter { file ->
            file.extension?.lowercase() == "hs"
        }.filter { file ->
            documentService.readDocument(path = file.path).map { doc ->
                doc.content.lineSequence().any { line ->
                    line.trim().startsWith("main =")
                }
            }.getOrElse { false }
        }.map { file ->
            LaunchTarget.File(name = file.nameWithoutExtension, workingDir = rootPath, filePath = file.path)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override suspend fun Raise<Throwable>.query(): Flow<Execution> {
        val configsFlow = vfsService.observeFiles(path = rootPath).bind().map { files ->
            files.filter { file ->
                val path = file.path.lowercase()

                !path.contains(".stack-work") && !path.contains("dist-newstyle")
            }
        }.distinctUntilChanged().map { files ->
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

        return configsFlow.flatMapLatest { discoveredConfigs ->
            combine(
                flow = toolchainService.toolchain,
                flow2 = executionService.configurations,
                flow3 = executionService.selectedConfiguration,
                flow4 = runtimeService.events.onStart<RuntimeEvent?> { emit(null) },
                transform = { _, savedConfigs, selected, _ ->
                    savedConfigs.ifEmpty { discoveredConfigs } to selected
                }).debounce(DEBOUNCE_MILLIS).map { (configs, selected) ->
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