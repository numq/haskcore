package io.github.numq.haskcore.cabal

import io.github.numq.haskcore.execution.ExecutionService
import java.nio.file.Files
import java.nio.file.Path

internal interface CabalService {
    suspend fun getCabalVersion(): Result<CabalVersion>

    suspend fun hasValidProject(path: String): Result<Boolean>

    class Default : CabalService, ExecutionService() {
        private val regex = """(?i)cabal-install\s+version\s+(\d+\.\d+\.\d+\.\d+)""".toRegex()

        override suspend fun getCabalVersion() = result(
            workingDir = ".", command = listOf("stack", "exec", "cabal", "--", "--version"), environment = emptyMap()
        ).mapCatching { result ->
            regex.find(result.output)?.groupValues?.get(1)?.trim()?.takeIf(String::isNotBlank)
                ?.let(CabalVersion::fromString)
                ?: throw CabalException("Empty or invalid Cabal version output")
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
    }
}