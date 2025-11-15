package io.github.numq.haskcore.buildsystem.ghc

import io.github.numq.haskcore.buildsystem.BuildCommand
import io.github.numq.haskcore.buildsystem.BuildOutput
import io.github.numq.haskcore.buildsystem.BuildSystemService
import io.github.numq.haskcore.buildsystem.exception.BuildSystemException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runInterruptible
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

internal interface GhcBuildSystemService {
    suspend fun getGhcVersion(): Result<String>

    suspend fun isValidFile(path: String): Result<Boolean>

    suspend fun execute(command: BuildCommand.Ghc): Result<Flow<BuildOutput>>

    class Default : BuildSystemService(), GhcBuildSystemService {
        private val regex =
            """(?i)(?:Glasgow Haskell Compilation System|ghc).*?version\s+(\d+\.\d+\.\d+(?:\.\d+)?)""".toRegex()

        override suspend fun getGhcVersion() = runCatching {
            ProcessBuilder("stack", "exec", "ghc", "--", "--version").redirectErrorStream(true).start()
                .also { process ->
                    val exitCode = runInterruptible(Dispatchers.IO) { process.waitFor() }

                    if (exitCode != 0) {
                        throw BuildSystemException("GHC version command failed with exit code: $exitCode")
                    }
                }.inputStream.bufferedReader().use { reader ->
                    reader.readText().trim()
                }.let { input ->
                    regex.find(input)?.groupValues?.get(1)
                }?.trim()?.takeIf(String::isNotBlank)
                ?: throw BuildSystemException("Empty or invalid GHC version output")
        }

        override suspend fun isValidFile(path: String) = runCatching {
            Path.of(path).run {
                isRegularFile() && extension == "hs"
            }
        }

        override suspend fun execute(command: BuildCommand.Ghc) = executeBuildCommand(command = command)
    }
}