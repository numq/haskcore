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
import java.io.File

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

        private fun extractProjectName(projectPath: String) = runCatching {
            val packageYamlPath = "$projectPath/$PACKAGE_YAML"

            if (fileSystemService.exists(packageYamlPath).getOrNull() == true) {
                fileSystemService.readLines(packageYamlPath).getOrNull()?.forEach { line ->
                    if (line.trim().startsWith("name:")) {
                        return@runCatching line.trim().removePrefix("name:").trim()
                    }
                }
            }

            fileSystemService.listDirectory(projectPath, recursive = false).getOrNull()?.firstOrNull { node ->
                node.name.endsWith(".cabal")
            }?.let { cabalFile ->
                fileSystemService.readLines(cabalFile.path).getOrNull()?.forEach { line ->
                    if (line.trim().startsWith("name:")) {
                        return@runCatching line.trim().removePrefix("name:").trim()
                    }
                }
            }

            null
        }

        private fun extractDependencies(projectPath: String) = runCatching {
            buildList {
                val packageYamlPath = "$projectPath/$PACKAGE_YAML"

                if (fileSystemService.exists(packageYamlPath).getOrThrow()) {
                    var inDependencies = false

                    fileSystemService.readLines(packageYamlPath).getOrNull()?.forEach { line ->
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

        private fun extractResolver(projectPath: String) = runCatching {
            val stackYamlPath = "$projectPath/$STACK_YAML"

            if (fileSystemService.exists(stackYamlPath).getOrNull() == true) {
                fileSystemService.readLines(stackYamlPath).getOrNull()?.forEach { line ->
                    if (line.trim().startsWith("resolver:")) {
                        return@runCatching line.trim().removePrefix("resolver:").trim()
                    }
                }
            }

            null
        }

        private fun parseBuildOutput(line: String) = when {
            line.contains("Building") -> parseBuildProgress(line)

            line.contains("Warning:") -> StackBuildOutput.Warning(message = line)

            line.contains("Error:") -> StackBuildOutput.Error(message = line)

            else -> StackBuildOutput.Progress(module = "unknown", message = line)
        }

        private fun parseBuildProgress(line: String) = with(BUILD_PATTERN) {
            val match = find(line)

            val module = match?.groupValues?.getOrNull(1) ?: "unknown"

            StackBuildOutput.Progress(module = module, message = line)
        }

        private fun parseTestOutput(line: String) = when {
            line.startsWith("PASS") || line.startsWith("FAIL") -> parseTestResult(line)

            line.contains("Warning:") -> StackTestOutput.Warning(line)

            line.contains("Error:") -> StackTestOutput.Error(line)

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
                null -> StackDependencyOutput.Info("unknown", "unknown")

                else -> StackDependencyOutput.Info(name = match.groupValues[1], version = match.groupValues[2])
            }
        }

        private suspend fun executeAndParseBuild(
            commands: List<String>, workingDirectory: String
        ) = processService.stream(commands, workingDirectory, emptyMap()).map { chunks ->
            chunks.map { chunk ->
                with(chunk) {
                    when (this) {
                        is ProcessOutputChunk.Line -> parseBuildOutput(text)

                        is ProcessOutputChunk.Completed -> StackBuildOutput.Completion(
                            exitCode = exitCode, duration = duration
                        )
                    }
                }
            }
        }

        private suspend fun executeAndParseTest(
            commands: List<String>, workingDirectory: String
        ) = processService.stream(commands, workingDirectory, emptyMap()).map { chunks ->
            chunks.map { chunk ->
                with(chunk) {
                    when (this) {
                        is ProcessOutputChunk.Line -> parseTestOutput(text)

                        is ProcessOutputChunk.Completed -> StackTestOutput.Completion(
                            exitCode = exitCode, duration = duration
                        )
                    }
                }
            }
        }

        private suspend fun executeAndParseRun(
            commands: List<String>, workingDirectory: String
        ) = processService.stream(commands, workingDirectory, emptyMap()).map { chunks ->
            chunks.map { chunk ->
                with(chunk) {
                    when (this) {
                        is ProcessOutputChunk.Line -> StackRunOutput.Output(text)

                        is ProcessOutputChunk.Completed -> StackRunOutput.Completion(
                            exitCode = exitCode, duration = duration
                        )
                    }
                }
            }
        }

        private suspend fun executeAndParseDependencies(
            commands: List<String>, workingDirectory: String
        ) = processService.stream(commands, workingDirectory, emptyMap()).map { chunks ->
            chunks.map { chunk ->
                with(chunk) {
                    when (this) {
                        is ProcessOutputChunk.Line -> parseDependencyOutput(text)

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
            VERSION_PATTERN.find(output.text)?.groupValues?.getOrNull(1)
        }

        override suspend fun getGhcVersion(path: String) = processService.execute(
            commands = withStack("ghc", "--", "--version"),
            workingDirectory = path,
        ).map { output ->
            VERSION_PATTERN.find(output.text)?.groupValues?.getOrNull(1)
        }

        override suspend fun getResolver(path: String) = runCatching {
            if (!fileSystemService.exists(path).getOrThrow() || !fileSystemService.isDirectory(path).getOrThrow()) {
                return@runCatching null
            }

            val stackYamlPath = "$path/$STACK_YAML"

            if (!fileSystemService.exists(stackYamlPath).getOrThrow()) {
                return@runCatching null
            }

            fileSystemService.readLines(stackYamlPath).getOrNull()?.forEach { line ->
                if (line.trim().startsWith("resolver:")) {
                    return@runCatching line.trim().removePrefix("resolver:").trim()
                }
            }

            null
        }

        override suspend fun getProject(path: String) = runCatching {
            if (!fileSystemService.exists(path).getOrThrow() || !fileSystemService.isDirectory(path).getOrThrow()) {
                return@runCatching StackProject.None(path = path)
            }

            val stackYamlPath = "$path/$STACK_YAML"

            if (!fileSystemService.exists(stackYamlPath).getOrThrow()) {
                return@runCatching StackProject.Incomplete(
                    path = path,
                    name = File(path).name,
                    missingRequirements = setOf(StackProject.Requirement.STACK)
                )
            }

            val nameResult = extractProjectName(path)

            val dependenciesResult = extractDependencies(path)

            val resolverResult = extractResolver(path)

            val ghcVersionResult = getGhcVersion(path)

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
            executeAndParseBuild(withStack("new", name, template.name), path)

        override suspend fun buildProject(path: String) = executeAndParseBuild(withStack("build"), path)

        override suspend fun runProject(path: String) = executeAndParseRun(withStack("run"), path)

        override suspend fun testProject(path: String) = executeAndParseTest(withStack("test"), path)

        override suspend fun cleanProject(path: String) = executeAndParseBuild(withStack("clean"), path)

        override suspend fun getDependencies(path: String) =
            executeAndParseDependencies(withStack("ls", "dependencies"), path)

        override suspend fun addDependency(path: String, dependency: String) =
            executeAndParseBuild(withStack("build", "--package", dependency), path)
    }
}