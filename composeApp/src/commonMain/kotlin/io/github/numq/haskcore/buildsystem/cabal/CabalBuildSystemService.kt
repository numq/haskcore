package io.github.numq.haskcore.buildsystem.cabal

import io.github.numq.haskcore.buildsystem.BuildCommand
import io.github.numq.haskcore.buildsystem.BuildOutput
import io.github.numq.haskcore.buildsystem.BuildSystemService
import io.github.numq.haskcore.buildsystem.exception.BuildSystemException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runInterruptible
import java.nio.file.Files
import java.nio.file.Path

internal interface CabalBuildSystemService {
    suspend fun getCabalVersion(): Result<String>

    suspend fun hasValidProject(path: String): Result<Boolean>

    suspend fun execute(command: BuildCommand.Cabal, requireProject: Boolean = true): Result<Flow<BuildOutput>>

    class Default : BuildSystemService(), CabalBuildSystemService {
        override val baseCommand = listOf("stack", "exec", "cabal")

        override suspend fun getCabalVersion() = runCatching {
            ProcessBuilder(*baseCommand.toTypedArray(), "--", "--version")
                .redirectErrorStream(true)
                .start()
                .also { process ->
                    val exitCode = runInterruptible(Dispatchers.IO) { process.waitFor() }

                    if (exitCode != 0) {
                        throw BuildSystemException("Cabal version command failed with exit code: $exitCode")
                    }
                }
                .inputStream
                .bufferedReader()
                .use { reader -> reader.readText().trim() }
                .lineSequence()
                .find { line -> line.contains("cabal-install version", ignoreCase = true) }
                ?.substringAfter("version")
                ?.trim()
                ?.takeIf(String::isNotBlank) ?: throw BuildSystemException("Empty or invalid Cabal version output")
        }

        override suspend fun hasValidProject(path: String) = runCatching {
            val workingDirectory = Path.of(path)

            if (!Files.isDirectory(workingDirectory)) return@runCatching false

            if (Files.exists(workingDirectory.resolve("cabal.project")) || Files.exists(workingDirectory.resolve("cabal.project.remote"))) {
                return@runCatching true
            }

            try {
                workingDirectory.toFile().listFiles { file ->
                    file.isFile && file.name.endsWith(".cabal")
                }?.isNotEmpty() == true
            } catch (_: SecurityException) {
                false
            }
        }

        override suspend fun execute(command: BuildCommand.Cabal, requireProject: Boolean) = runCatching {
            val path = command.path

            if (requireProject && !hasValidProject(path).getOrThrow()) {
                throw BuildSystemException("Not a cabal project: $path")
            }

            executeBuildCommand(command = command).getOrThrow()
        }
    }
}