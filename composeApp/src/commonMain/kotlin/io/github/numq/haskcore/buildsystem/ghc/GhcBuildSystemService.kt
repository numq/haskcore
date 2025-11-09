package io.github.numq.haskcore.buildsystem.ghc

import io.github.numq.haskcore.buildsystem.BuildCommand
import io.github.numq.haskcore.buildsystem.BuildOutput
import io.github.numq.haskcore.buildsystem.BuildSystemService
import io.github.numq.haskcore.buildsystem.exception.BuildSystemException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runInterruptible
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

internal interface GhcBuildSystemService {
    suspend fun getGhcVersion(): Result<String>

    suspend fun isValidFile(path: String): Result<Boolean>

    suspend fun execute(command: BuildCommand.Ghc): Result<Flow<BuildOutput>>

    class Default : BuildSystemService(), GhcBuildSystemService {
        override val baseCommand: List<String> = listOf("stack", "exec", "ghc")

        override suspend fun getGhcVersion() = runCatching {
            ProcessBuilder(*baseCommand.toTypedArray(), "--", "--version")
                .redirectErrorStream(true)
                .start()
                .also { process ->
                    val exitCode = runInterruptible(Dispatchers.IO) { process.waitFor() }

                    if (exitCode != 0) {
                        throw BuildSystemException("GHC version command failed with exit code: $exitCode")
                    }
                }
                .inputStream
                .bufferedReader().use { reader -> reader.readText().trim() }
                .lineSequence()
                .firstOrNull()
                ?.substringAfter("version")
                ?.trim()
                ?.takeIf(String::isNotBlank) ?: throw BuildSystemException("Empty or invalid GHC version output")
        }

        override suspend fun isValidFile(path: String) = runCatching {
            Paths.get(path).run {
                isRegularFile() && extension == "hs"
            }
        }

        override suspend fun execute(command: BuildCommand.Ghc) = executeBuildCommand(command = command)
    }
}