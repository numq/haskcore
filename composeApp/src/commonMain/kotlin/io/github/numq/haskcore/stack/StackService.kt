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
import kotlin.io.path.name

interface StackService {
    suspend fun hasStack(): Result<Boolean>

    suspend fun getStackVersion(): Result<String?>

    suspend fun getGhcVersion(path: String): Result<String?>

    suspend fun getResolver(path: String): Result<String?>

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
            const val STACK_COMMAND = "stack"

            const val STACK_YAML = "stack.yaml"

            const val PACKAGE_YAML = "package.yaml"

            val VERSION_PATTERN by lazy { Regex("""[Vv]ersion (\d+\.\d+\.\d+)""") }

            val BUILD_PATTERN by lazy { Regex("""Building\s+([\w-]+)""") }

            val TEST_PATTERN by lazy { Regex("""(PASS|FAIL):\s+([\w.]+)""") }

            val DEPENDENCY_PATTERN by lazy { Regex("""([\w-]+)-(\d+\.\d+\.\d+\.?\d*)""") }
        }

        private fun withStack(vararg args: String) = listOf(STACK_COMMAND, *args, "--color=never")

        private suspend fun extractProjectName(projectPath: String) = runCatching {
            val packageYamlPath = Path(projectPath, PACKAGE_YAML).absolutePathString()

            if (fileSystemService.exists(path = packageYamlPath).getOrNull() == true) {
                fileSystemService.readLines(path = packageYamlPath).getOrNull()?.forEach { line ->
                    if (line.trim().startsWith("name:")) {
                        return@runCatching line.trim().removePrefix("name:").trim()
                    }
                }
            }

            fileSystemService.listDirectory(projectPath).getOrNull()?.firstOrNull { path ->
                path.endsWith(".cabal")
            }?.let { path ->
                fileSystemService.readLines(path = path).getOrNull()?.forEach { line ->
                    if (line.trim().startsWith("name:")) {
                        return@runCatching line.trim().removePrefix("name:").trim()
                    }
                }
            }

            null
        }

        private suspend fun extractDependencies(projectPath: String) = runCatching {
            buildList {
                val packageYamlPath = Path(projectPath, PACKAGE_YAML).absolutePathString()

                if (fileSystemService.exists(path = packageYamlPath).getOrThrow()) {
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
        }

        private suspend fun extractResolver(projectPath: String) = runCatching {
            val stackYamlPath = Path(projectPath, STACK_YAML).absolutePathString()

            if (fileSystemService.exists(path = stackYamlPath).getOrNull() == true) {
                fileSystemService.readLines(path = stackYamlPath).getOrNull()?.forEach { line ->
                    if (line.trim().startsWith("resolver:")) {
                        return@runCatching line.trim().removePrefix("resolver:").trim()
                    }
                }
            }

            null
        }

        private fun parseBuildOutput(line: String) = when {
            line.contains("Building") -> parseBuildProgress(line = line)

            line.contains("Warning:") -> StackBuildOutput.Warning(message = line)

            line.contains("Error:") -> StackBuildOutput.Error(message = line)

            else -> StackBuildOutput.Progress(module = "unknown", message = line)
        }

        private fun parseBuildProgress(line: String) = with(BUILD_PATTERN) {
            val match = find(input = line)

            val module = match?.groupValues?.getOrNull(1) ?: "unknown"

            StackBuildOutput.Progress(module = module, message = line)
        }

        private fun parseTestOutput(line: String) = when {
            line.startsWith("PASS") || line.startsWith("FAIL") -> parseTestResult(line)

            line.contains("Warning:") -> StackTestOutput.Warning(message = line)

            line.contains("Error:") -> StackTestOutput.Error(message = line)

            else -> StackTestOutput.Result(module = "unknown", passed = false, details = line)
        }

        private fun parseTestResult(line: String) = with(TEST_PATTERN) {
            val match = find(line)

            val passed = match?.groupValues?.getOrNull(1) == "PASS"

            val module = match?.groupValues?.getOrNull(2) ?: "unknown"

            StackTestOutput.Result(module = module, passed = passed, details = line)
        }

        private fun parseDependencyOutput(line: String) = with(DEPENDENCY_PATTERN) {
            when (val match = find(line)) {
                null -> StackDependencyOutput.Info(name = "unknown", version = "unknown")

                else -> StackDependencyOutput.Info(name = match.groupValues[1], version = match.groupValues[2])
            }
        }

        private suspend fun executeAndParseBuild(
            commands: List<String>, workingDirectory: String
        ) = processService.stream(
            commands = commands, workingDirectory = workingDirectory, environment = emptyMap()
        ).map { chunks ->
            chunks.map { chunk ->
                with(chunk) {
                    when (this) {
                        is ProcessOutputChunk.Line -> parseBuildOutput(line = text)

                        is ProcessOutputChunk.Completed -> StackBuildOutput.Completion(
                            exitCode = exitCode, duration = duration
                        )
                    }
                }
            }
        }

        private suspend fun executeAndParseTest(
            commands: List<String>, workingDirectory: String
        ) = processService.stream(
            commands = commands, workingDirectory = workingDirectory, environment = emptyMap()
        ).map { chunks ->
            chunks.map { chunk ->
                with(chunk) {
                    when (this) {
                        is ProcessOutputChunk.Line -> parseTestOutput(line = text)

                        is ProcessOutputChunk.Completed -> StackTestOutput.Completion(
                            exitCode = exitCode, duration = duration
                        )
                    }
                }
            }
        }

        private suspend fun executeAndParseRun(
            commands: List<String>, workingDirectory: String
        ) = processService.stream(
            commands = commands, workingDirectory = workingDirectory, environment = emptyMap()
        ).map { chunks ->
            chunks.map { chunk ->
                with(chunk) {
                    when (this) {
                        is ProcessOutputChunk.Line -> StackRunOutput.Output(text = text)

                        is ProcessOutputChunk.Completed -> StackRunOutput.Completion(
                            exitCode = exitCode, duration = duration
                        )
                    }
                }
            }
        }

        private suspend fun executeAndParseDependencies(
            commands: List<String>, workingDirectory: String
        ) = processService.stream(
            commands = commands, workingDirectory = workingDirectory, environment = emptyMap()
        ).map { chunks ->
            chunks.map { chunk ->
                with(chunk) {
                    when (this) {
                        is ProcessOutputChunk.Line -> parseDependencyOutput(line = text)

                        is ProcessOutputChunk.Completed -> StackDependencyOutput.Completion(
                            exitCode = exitCode, duration = duration
                        )
                    }
                }
            }
        }

        override suspend fun hasStack() = processService.execute(
            commands = withStack("--version"),
            workingDirectory = ".",
        ).map { output ->
            output.exitCode == 0
        }

        override suspend fun getStackVersion() = processService.execute(
            commands = withStack("--version"),
            workingDirectory = ".",
        ).map { output ->
            VERSION_PATTERN.find(input = output.text)?.groupValues?.getOrNull(1)
        }

        override suspend fun getGhcVersion(path: String) = processService.execute(
            commands = withStack("ghc", "--", "--version"),
            workingDirectory = path,
        ).map { output ->
            VERSION_PATTERN.find(input = output.text)?.groupValues?.getOrNull(1)
        }

        override suspend fun getResolver(path: String) = runCatching {
            if (!fileSystemService.exists(path = path).getOrThrow() || !fileSystemService.isDirectory(path = path)
                    .getOrThrow()
            ) {
                return@runCatching null
            }

            val stackYamlPath = Path(path, STACK_YAML).absolutePathString()

            if (!fileSystemService.exists(path = stackYamlPath).getOrThrow()) {
                return@runCatching null
            }

            fileSystemService.readLines(path = stackYamlPath).getOrNull()?.forEach { line ->
                if (line.trim().startsWith("resolver:")) {
                    return@runCatching line.trim().removePrefix("resolver:").trim()
                }
            }

            null
        }

        override suspend fun getProject(path: String) = runCatching {
            if (!fileSystemService.exists(path = path).getOrThrow() || !fileSystemService.isDirectory(path = path)
                    .getOrThrow()
            ) {
                return@runCatching StackProject.None(path = path)
            }

            val stackYamlPath = Path(path, STACK_YAML).absolutePathString()

            if (!fileSystemService.exists(path = stackYamlPath).getOrThrow()) {
                val name = Path(path).name

                return@runCatching StackProject.Incomplete(
                    path = path, name = name, missingRequirements = setOf(StackProject.Requirement.STACK)
                )
            }

            val nameResult = extractProjectName(projectPath = path)

            val dependenciesResult = extractDependencies(projectPath = path)

            val resolverResult = extractResolver(projectPath = path)

            val ghcVersionResult = getGhcVersion(path = path)

            val errors = mutableListOf<String>()

            val missingRequirements = mutableSetOf<StackProject.Requirement>()

            val name = nameResult.onFailure { throwable ->
                errors.add("Failed to get project name: ${throwable.message}")
            }.getOrNull() ?: "unknown"

            val dependencies = dependenciesResult.onFailure {
                errors.add("Failed to get dependencies: ${it.message}")
            }.getOrDefault(emptyList())

            val resolver = resolverResult.onFailure {
                errors.add("Failed to get resolver: ${it.message}")
            }.getOrNull()

            if (resolver == null) {
                missingRequirements.add(StackProject.Requirement.RESOLVER)
            }

            val ghcVersion = ghcVersionResult.onFailure {
                errors.add("Failed to get GHC version: ${it.message}")
            }.getOrNull()

            if (ghcVersion == null) {
                missingRequirements.add(StackProject.Requirement.GHC_VERSION)
            }

            when {
                errors.isNotEmpty() -> StackProject.Invalid(path = path, name = name, errors = errors)

                resolver != null && ghcVersion != null -> StackProject.Valid(
                    path = path,
                    name = name,
                    dependencies = dependencies,
                    resolver = resolver,
                    ghcVersion = ghcVersion,
                )

                else -> StackProject.Incomplete(path = path, name = name, missingRequirements = missingRequirements)
            }
        }

        override suspend fun createProject(name: String, path: String, template: StackTemplate) =
            executeAndParseBuild(commands = withStack("new", name, template.name), workingDirectory = path)

        override suspend fun buildProject(path: String) =
            executeAndParseBuild(commands = withStack("build"), workingDirectory = path)

        override suspend fun runProject(path: String) =
            executeAndParseRun(commands = withStack("run"), workingDirectory = path)

        override suspend fun testProject(path: String) =
            executeAndParseTest(commands = withStack("test"), workingDirectory = path)

        override suspend fun cleanProject(path: String) =
            executeAndParseBuild(commands = withStack("clean"), workingDirectory = path)

        override suspend fun getDependencies(path: String) =
            executeAndParseDependencies(commands = withStack("ls", "dependencies"), workingDirectory = path)

        override suspend fun addDependency(path: String, dependency: String) =
            executeAndParseBuild(commands = withStack("build", "--package", dependency), workingDirectory = path)
    }
}