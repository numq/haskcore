package io.github.numq.haskcore.buildsystem.stack

import io.github.numq.haskcore.buildsystem.BuildCommand
import io.github.numq.haskcore.buildsystem.BuildOutput
import io.github.numq.haskcore.buildsystem.BuildSystemService
import io.github.numq.haskcore.buildsystem.exception.BuildSystemException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runInterruptible
import java.nio.file.Files
import java.nio.file.Path

internal interface StackBuildSystemService {
    suspend fun getStackVersion(): Result<String>

    suspend fun hasValidProject(path: String): Result<Boolean>

    suspend fun execute(command: BuildCommand.Stack, requireProject: Boolean = true): Result<Flow<BuildOutput>>

    class Default : BuildSystemService(), StackBuildSystemService {
        override val baseCommand = listOf("stack")

        override suspend fun getStackVersion() = runCatching {
            ProcessBuilder(*baseCommand.toTypedArray(), "--version")
                .redirectErrorStream(true)
                .start()
                .also { process ->
                    val exitCode = runInterruptible(Dispatchers.IO) { process.waitFor() }

                    if (exitCode != 0) {
                        throw BuildSystemException("Stack version command failed with exit code: $exitCode")
                    }
                }
                .inputStream
                .bufferedReader()
                .use { reader -> reader.readText().trim() }
                .substringAfter("Version")
                .substringBefore(",")
                .trim()
                .takeIf(String::isNotBlank) ?: throw BuildSystemException("Empty or invalid Stack version output")
        }

        override suspend fun hasValidProject(path: String) = runCatching {
            val workingDirectory = Path.of(path)

            Files.isDirectory(workingDirectory) && Files.exists(workingDirectory.resolve("stack.yaml"))
        }

        override suspend fun execute(command: BuildCommand.Stack, requireProject: Boolean) = runCatching {
            val path = command.path

            if (requireProject && !hasValidProject(path).getOrThrow()) {
                throw BuildSystemException("Not a stack project: $path")
            }

            executeBuildCommand(command = command).getOrThrow()
        }
    }
}