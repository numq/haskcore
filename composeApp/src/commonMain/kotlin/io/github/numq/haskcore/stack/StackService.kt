package io.github.numq.haskcore.stack

import io.github.numq.haskcore.filesystem.FileSystemService
import io.github.numq.haskcore.process.ProcessOutputChunk
import io.github.numq.haskcore.process.ProcessService
import io.github.numq.haskcore.stack.output.StackBuildOutput
import io.github.numq.haskcore.stack.output.StackDependencyOutput
import io.github.numq.haskcore.stack.output.StackRunOutput
import io.github.numq.haskcore.stack.output.StackTestOutput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

internal interface StackService {
    suspend fun getProject(path: String): Result<StackProject>

    suspend fun createProject(name: String, path: String, template: StackTemplate): Result<Flow<StackBuildOutput>>

    suspend fun buildProject(path: String): Result<Flow<StackBuildOutput>>

    suspend fun runProject(path: String): Result<Flow<StackRunOutput>>

    suspend fun testProject(path: String): Result<Flow<StackTestOutput>>

    suspend fun cleanProject(path: String): Result<Flow<StackBuildOutput>>

    suspend fun getDependencies(path: String): Result<Flow<StackDependencyOutput>>

    suspend fun addDependency(path: String, dependency: String): Result<Flow<StackBuildOutput>>

    class Default(
        private val fileSystemService: FileSystemService, private val processService: ProcessService
    ) : StackService {
        private companion object {
            val GHC_VERSION_PATTERN = Regex("""version (\d+\.\d+\.\d+)""")

            val BUILD_PATTERN = Regex("""Building\s+([\w-]+)""")

            val TEST_PATTERN = Regex("""(PASS|FAIL):\s+([\w.]+)""")

            val DEPENDENCY_PATTERN = Regex("""([\w-]+)-(\d+\.\d+\.\d+\.?\d*)""")
        }

        private suspend fun parseProjectName(projectPath: String): String = runCatching {
            val packageYamlPath = Path(projectPath, "package.yaml").absolutePathString()

            if (fileSystemService.exists(path = packageYamlPath).getOrNull() == true) {
                fileSystemService.readLines(path = packageYamlPath).getOrNull()?.forEach { line ->
                    if (line.trim().startsWith("name:")) {
                        return@runCatching line.trim().removePrefix("name:").trim()
                    }
                }
            }

            fileSystemService.listDirectory(path = projectPath).getOrNull()?.firstOrNull { path ->
                path.endsWith(".cabal")
            }?.let { cabalPath ->
                val fullCabalPath = Path(projectPath, cabalPath).absolutePathString()
                fileSystemService.readLines(path = fullCabalPath).getOrNull()?.forEach { line ->
                    if (line.trim().startsWith("name:")) {
                        return@runCatching line.trim().removePrefix("name:").trim()
                    }
                }
            }

            "unknown"
        }.getOrElse { "unknown" }

        private fun parseGhcVersion(text: String) = GHC_VERSION_PATTERN.find(text)?.groupValues?.get(1)

        private suspend fun parseResolver(projectPath: String): String? {
            val stackYamlPath = Path(projectPath, "stack.yaml").absolutePathString()

            if (fileSystemService.exists(path = stackYamlPath).getOrNull() == true) {
                fileSystemService.readLines(path = stackYamlPath).getOrNull()?.forEach { line ->
                    if (line.trim().startsWith("resolver:")) {
                        return line.trim().removePrefix("resolver:").trim()
                    }
                }
            }

            return null
        }

        private suspend fun parseDependencies(projectPath: String): List<String> = runCatching {
            buildList {
                val packageYamlPath = Path(projectPath, "package.yaml").absolutePathString()

                if (fileSystemService.exists(path = packageYamlPath).getOrNull() == true) {
                    var inDependencies = false

                    fileSystemService.readLines(path = packageYamlPath).getOrNull()?.forEach { line ->
                        val trimmed = line.trim()

                        when {
                            trimmed == "dependencies:" -> inDependencies = true

                            inDependencies && trimmed.startsWith("-") -> add(trimmed.removePrefix("-").trim())

                            inDependencies && trimmed.isNotEmpty() && !trimmed.startsWith("-") -> inDependencies = false
                        }
                    }
                }
            }
        }.getOrElse { emptyList() }

        private fun parseDependencyOutput(chunk: ProcessOutputChunk): StackDependencyOutput = when (chunk) {
            is ProcessOutputChunk.Line -> when (val match = DEPENDENCY_PATTERN.find(chunk.text)) {
                null -> StackDependencyOutput.Info(name = "unknown", version = "unknown")

                else -> StackDependencyOutput.Info(name = match.groupValues[1], version = match.groupValues[2])
            }

            is ProcessOutputChunk.Completed -> StackDependencyOutput.Completion(
                exitCode = chunk.exitCode, duration = chunk.duration
            )
        }

        private fun parseBuildProgress(line: String): StackBuildOutput.Progress {
            val match = BUILD_PATTERN.find(line)

            val module = match?.groupValues?.getOrNull(1) ?: "unknown"

            return StackBuildOutput.Progress(module = module, message = line)
        }

        private fun parseBuildOutput(chunk: ProcessOutputChunk) = when (chunk) {
            is ProcessOutputChunk.Line -> when {
                chunk.text.contains("Building") -> parseBuildProgress(line = chunk.text)

                chunk.text.contains("Warning:") -> StackBuildOutput.Warning(message = chunk.text)

                chunk.text.contains("Error:") -> StackBuildOutput.Error(message = chunk.text)

                else -> StackBuildOutput.Progress(module = "unknown", message = chunk.text)
            }

            is ProcessOutputChunk.Completed -> StackBuildOutput.Completion(
                exitCode = chunk.exitCode, duration = chunk.duration
            )
        }

        private fun parseRunOutput(chunk: ProcessOutputChunk) = when (chunk) {
            is ProcessOutputChunk.Line -> StackRunOutput.Output(text = chunk.text)

            is ProcessOutputChunk.Completed -> StackRunOutput.Completion(
                exitCode = chunk.exitCode, duration = chunk.duration
            )
        }

        private fun parseTestOutput(chunk: ProcessOutputChunk) = when (chunk) {
            is ProcessOutputChunk.Line -> when {
                chunk.text.startsWith("PASS") || chunk.text.startsWith("FAIL") -> parseTestResult(line = chunk.text)

                chunk.text.contains("Warning:") -> StackTestOutput.Warning(message = chunk.text)

                chunk.text.contains("Error:") -> StackTestOutput.Error(message = chunk.text)

                else -> StackTestOutput.Result(module = "unknown", passed = false, output = chunk.text)
            }

            is ProcessOutputChunk.Completed -> StackTestOutput.Completion(
                exitCode = chunk.exitCode, duration = chunk.duration
            )
        }

        private fun parseTestResult(line: String): StackTestOutput.Result {
            val match = TEST_PATTERN.find(line)

            val passed = match?.groupValues?.getOrNull(1) == "PASS"

            val module = match?.groupValues?.getOrNull(2) ?: "unknown"

            return StackTestOutput.Result(module = module, passed = passed, output = line)
        }

        private suspend fun getGhcVersion(path: String) = processService.execute(
            commands = listOf("stack", "ghc", "--", "--version"), workingDirectory = path
        ).getOrNull()?.let { output ->
            parseGhcVersion(text = output.text)
        }

        private suspend fun isValidDirectory(path: String) =
            fileSystemService.isDirectory(path = path).getOrNull() == true

        private suspend fun hasStackYaml(path: String) =
            fileSystemService.exists(path = Path(path, "stack.yaml").absolutePathString()).getOrNull() == true

        private suspend fun <T> executeAndParse(
            path: String, args: Array<out String>, parser: (ProcessOutputChunk) -> T
        ) = processService.stream(
            commands = listOf("stack", *args, "--color=never"), workingDirectory = path, environment = emptyMap()
        ).mapCatching { chunks -> chunks.map(parser) }

        override suspend fun getProject(path: String) = runCatching {
            val name = parseProjectName(path)

            if (!isValidDirectory(path = path)) {
                error("Invalid directory")
            }

            if (!hasStackYaml(path = path)) {
                error("No stack.yaml found")
            }

            val resolver = parseResolver(projectPath = path) ?: error("No resolver found")

            val ghcVersion = getGhcVersion(path = path) ?: error("No GHC found")

            StackProject(
                path = path,
                name = name,
                resolver = resolver,
                ghcVersion = ghcVersion,
                dependencies = parseDependencies(projectPath = path)
            )
        }

        override suspend fun createProject(name: String, path: String, template: StackTemplate) =
            executeAndParse(path = path, args = arrayOf("new", name, template.name), parser = ::parseBuildOutput)

        override suspend fun buildProject(path: String) =
            executeAndParse(path = path, args = arrayOf("build"), parser = ::parseBuildOutput)

        override suspend fun runProject(path: String) =
            executeAndParse(path = path, args = arrayOf("run"), parser = ::parseRunOutput)

        override suspend fun testProject(path: String) =
            executeAndParse(path = path, args = arrayOf("test"), parser = ::parseTestOutput)

        override suspend fun cleanProject(path: String) =
            executeAndParse(path = path, args = arrayOf("clean"), parser = ::parseBuildOutput)

        override suspend fun getDependencies(path: String) =
            executeAndParse(path = path, args = arrayOf("ls", "dependencies"), parser = ::parseDependencyOutput)

        override suspend fun addDependency(path: String, dependency: String) =
            executeAndParse(path = path, args = arrayOf("build", "--package", dependency), parser = ::parseBuildOutput)
    }
}