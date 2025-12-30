package io.github.numq.haskcore.ghc

import io.github.numq.haskcore.execution.ExecutionService
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

internal interface GhcService {
    suspend fun getGhcVersion(): Result<GhcVersion>

    suspend fun isValidFile(path: String): Result<Boolean>

    suspend fun isValidScript(path: String): Result<Boolean>

    class Default : GhcService, ExecutionService() {
        private val regex =
            """(?i)(?:Glasgow Haskell Compilation System|ghc).*?version\s+(\d+\.\d+\.\d+(?:\.\d+)?)""".toRegex()

        override suspend fun getGhcVersion() = result(
            workingDir = ".", command = listOf("stack", "exec", "ghc", "--", "--version"), environment = emptyMap()
        ).mapCatching { result ->
            regex.find(result.output)?.groupValues?.get(1)?.trim()?.takeIf(String::isNotBlank)
                ?.let(GhcVersion::fromString)
                ?: throw GhcException("Empty or invalid GHC version output")
        }

        override suspend fun isValidFile(path: String) = runCatching {
            Path.of(path).run {
                isRegularFile() && extension == "hs"
            }
        }

        override suspend fun isValidScript(path: String) = runCatching {
            Path.of(path).run {
                isRegularFile() && extension == "lhs"
            }
        }
    }
}