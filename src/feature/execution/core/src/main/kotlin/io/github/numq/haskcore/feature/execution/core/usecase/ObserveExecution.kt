package io.github.numq.haskcore.feature.execution.core.usecase

import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.toNonEmptyListOrNull
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.execution.core.Execution
import io.github.numq.haskcore.feature.execution.core.ExecutionArtifact
import io.github.numq.haskcore.feature.execution.core.ExecutionService
import io.github.numq.haskcore.feature.execution.core.ExecutionTarget
import io.github.numq.haskcore.service.runtime.RuntimeEvent
import io.github.numq.haskcore.service.runtime.RuntimeRequest
import io.github.numq.haskcore.service.runtime.RuntimeService
import io.github.numq.haskcore.service.toolchain.Toolchain
import io.github.numq.haskcore.service.toolchain.ToolchainService
import io.github.numq.haskcore.service.vfs.VfsService
import io.github.numq.haskcore.service.vfs.VirtualFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class ObserveExecution(
    private val path: String,
    private val executionService: ExecutionService,
    private val runtimeService: RuntimeService,
    private val toolchainService: ToolchainService,
    private val vfsService: VfsService,
) : UseCase<Unit, Flow<Execution>> {
    private companion object {
        const val PROBE_CABAL = "cabal"

        const val PROBE_STACK = "stack"
    }

    private fun createTarget(file: File): ExecutionTarget {
        val path = file.absolutePath

        val name = file.nameWithoutExtension

        val extension = file.extension

        return when {
            path.contains("test", ignoreCase = true) || name.contains(
                "test", ignoreCase = true
            ) -> ExecutionTarget.Test(path = path, name = name, extension = extension)

            path.contains("bench", ignoreCase = true) || name.contains(
                "bench", ignoreCase = true
            ) -> ExecutionTarget.Benchmark(path = path, name = name, extension = extension)

            else -> ExecutionTarget.Executable(path = path, name = name, extension = extension)
        }
    }

    private suspend fun fetchArtifacts(toolchain: Toolchain, virtualFiles: List<VirtualFile>) = either {
        when (toolchain) {
            is Toolchain.Detected -> coroutineScope {
                val cabal = toolchain.cabal.map {
                    val request = RuntimeRequest.Cabal(
                        id = PROBE_CABAL, name = PROBE_CABAL, arguments = listOf("list-bin", "all")
                    )

                    runtimeService.execute(request = request).bind().filterIsInstance<RuntimeEvent.Stdout>()
                        .map { event ->
                            val file = File(event.text)

                            ExecutionArtifact.Cabal(target = createTarget(file = file))
                        }
                }.bind()

                val stack = toolchain.stack.map {
                    val request = RuntimeRequest.Stack(
                        id = PROBE_STACK, name = PROBE_STACK, arguments = listOf("ide", "targets")
                    )

                    runtimeService.execute(request = request).bind().filterIsInstance<RuntimeEvent.Stdout>()
                        .map { event ->
                            val file = File(event.text)

                            ExecutionArtifact.Stack(target = createTarget(file))
                        }
                }.bind()

                val vfs = virtualFiles.mapNotNull { virtualFile ->
                    val file = File(virtualFile.path)

                    when {
                        !file.isDirectory && !file.path.contains("dist-newstyle") && !file.path.contains(".stack-work") -> {
                            val target = createTarget(file)

                            val artifact = when (file.extension.lowercase()) {
                                "hs" -> ExecutionArtifact.File.Haskell(target = target)

                                "lhs" -> ExecutionArtifact.File.LiterateScript(target = target)

                                else -> null
                            }

                            artifact
                        }

                        else -> null
                    }
                }.asFlow()

                merge(cabal, stack, vfs).toList()
            }

            else -> emptyList()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override suspend fun Raise<Throwable>.execute(input: Unit) = combine(
        flow = toolchainService.toolchain, flow2 = vfsService.observeDirectory(path = path).bind(), transform = ::Pair
    ).distinctUntilChanged { old, new ->
        val toolchainSame = old.first == new.first

        val filesSame = old.second.map(VirtualFile::path) == new.second.map(VirtualFile::path)

        toolchainSame && filesSame
    }.debounce(500.milliseconds).transformLatest { (toolchain, virtualFiles) ->
        emit(Execution.Syncing)

        val artifacts = fetchArtifacts(toolchain = toolchain, virtualFiles = virtualFiles).bind().toNonEmptyListOrNull()

        val execution = when (artifacts) {
            null -> Execution.Synced.NotFound

            else -> Execution.Synced.Found.Stopped(artifacts = artifacts)
        }

        emit(execution)
    }.combine(flow = executionService.selectedArtifactPath, transform = { execution, selectedArtifactPath ->
        when (execution) {
            is Execution.Synced.Found.Stopped -> execution.copy(selectedArtifact = execution.artifacts.find { artifact ->
                artifact.target.path == selectedArtifactPath
            } ?: execution.artifacts.head)

            else -> execution
        }
    })
}