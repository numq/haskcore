package io.github.numq.haskcore.feature.execution.core.usecase

import arrow.core.raise.Raise
import arrow.core.toNonEmptyListOrNull
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.execution.core.Execution
import io.github.numq.haskcore.feature.execution.core.ExecutionArtifact
import io.github.numq.haskcore.feature.execution.core.ExecutionService
import io.github.numq.haskcore.feature.execution.core.ExecutionTarget
import io.github.numq.haskcore.service.toolchain.ToolchainService
import io.github.numq.haskcore.service.vfs.VfsService
import io.github.numq.haskcore.service.vfs.VirtualFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class ObserveExecution(
    private val rootPath: String,
    private val executionService: ExecutionService,
    private val toolchainService: ToolchainService,
    private val vfsService: VfsService,
) : UseCase<Unit, Flow<Execution>> {
    private fun createTarget(file: File, rootPath: String): ExecutionTarget {
        val absolutePath = file.absolutePath

        val name = file.nameWithoutExtension

        val extension = file.extension

        val relativePath = absolutePath.removePrefix(rootPath).lowercase()

        return when {
            relativePath.contains("${File.separator}test${File.separator}") || name.contains(
                "test", ignoreCase = true
            ) -> ExecutionTarget.Test(path = absolutePath, name = name, extension = extension)

            relativePath.contains("${File.separator}bench${File.separator}") || name.contains(
                "bench", ignoreCase = true
            ) -> ExecutionTarget.Benchmark(path = absolutePath, name = name, extension = extension)

            else -> ExecutionTarget.Executable(path = absolutePath, name = name, extension = extension)
        }
    }

    private fun findAllExecutables(root: String) = File(root).walkTopDown().filter { file ->
        file.extension == "hs" && !file.path.contains("dist-newstyle") && !file.path.contains(".stack-work") && !file.path.contains(
            ".cabal-sandbox"
        )
    }.filter { file ->
        runCatching {
            file.useLines { lines ->
                lines.any { line ->
                    line.contains("main =") || line.contains("main ::")
                }
            }
        }.getOrDefault(false)
    }.map { file ->
        ExecutionArtifact.File.Haskell(target = createTarget(file = file, rootPath = root))
    }.toList()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override suspend fun Raise<Throwable>.execute(input: Unit) = combine(
        flow = toolchainService.toolchain,
        flow2 = vfsService.observeVisibleFiles(path = rootPath).bind(),
        transform = ::Pair
    ).distinctUntilChanged { old, new ->
        val toolchainSame = old.first == new.first

        val oldPaths = old.second.map(VirtualFile::path).toSet()

        val newPaths = new.second.map(VirtualFile::path).toSet()

        val filesSame = oldPaths == newPaths

        toolchainSame && filesSame
    }.debounce(500.milliseconds).transform {
        emit(Execution.Syncing)

        val artifacts = findAllExecutables(root = rootPath).toNonEmptyListOrNull()

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