package io.github.numq.haskcore.stack

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runInterruptible
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.milliseconds

internal interface StackService {
    suspend fun getProject(path: String): Result<StackProject>

    suspend fun create(path: String, name: String, template: String): Result<Flow<StackOutput>>

    suspend fun build(path: String): Result<Flow<StackOutput>>

    suspend fun run(path: String): Result<Flow<StackOutput>>

    suspend fun test(path: String): Result<Flow<StackOutput>>

    suspend fun clean(path: String): Result<Flow<StackOutput>>

    class Default : StackService {
        private companion object {
            val GHC_VERSION_PATTERN = Regex("""version (\d+\.\d+\.\d+)""")

            val BUILD_PATTERN = Regex("""Building\s+([\w-]+)""")

            val TEST_PATTERN = Regex("""(PASS|FAIL):\s+([\w.]+)""")
        }

        private fun parseBuildProgress(line: String): StackOutput {
            val match = BUILD_PATTERN.find(line)

            val module = match?.groupValues?.getOrNull(1) ?: "unknown"

            return StackOutput.BuildModule(module, line)
        }

        private fun parseTestResult(line: String): StackOutput {
            val match = TEST_PATTERN.find(line)

            val passed = match?.groupValues?.getOrNull(1) == "PASS"

            val module = match?.groupValues?.getOrNull(2) ?: "unknown"

            return StackOutput.TestResult(module, passed)
        }

        private fun parseStackOutput(line: String, operation: String) = when {
            line.contains("Building") -> parseBuildProgress(line)

            line.startsWith("PASS") || line.startsWith("FAIL") -> parseTestResult(line)

            line.contains("Warning:") -> StackOutput.Warning(line)

            line.contains("Error:") -> StackOutput.Error(line)

            operation == "run" -> StackOutput.RunOutput(line)

            else -> StackOutput.Progress(line)
        }

        private fun executeStackCommand(
            workingDirectory: Path, args: Array<out String>, operation: String
        ) = callbackFlow {
            val startTime = System.currentTimeMillis()

            val process = ProcessBuilder(listOf("stack", *args, "--color=never")).run {
                directory(workingDirectory.toFile())

                redirectErrorStream(true)

                start()
            }

            val reader = process.inputStream.bufferedReader()

            var lineCount = 0

            try {
                while (isActive) {
                    val line = reader.readLine() ?: break

                    lineCount++

                    val stackOutput = parseStackOutput(line, operation)

                    send(stackOutput)
                }

                val exitCode = runInterruptible { process.waitFor() }

                val duration = (System.currentTimeMillis() - startTime).milliseconds

                val completion = when (exitCode) {
                    0 -> StackOutput.Completion.Success(exitCode = exitCode, duration = duration)

                    else -> StackOutput.Completion.Failure(
                        exitCode = exitCode, duration = duration, error = "$operation failed with exit code $exitCode"
                    )
                }

                send(completion)
            } catch (exception: CancellationException) {
                throw exception
            } catch (throwable: Throwable) {
                throw StackException("Failed to execute stack command: ${throwable.message}")
            }

            awaitClose {
                process?.destroy()
            }
        }.flowOn(Dispatchers.IO)

        private fun parseProjectName(projectPath: Path): String {
            val stackYaml = projectPath.resolve("stack.yaml")

            if (Files.exists(stackYaml)) {
                Files.readAllLines(stackYaml).forEach { line ->
                    if (line.trim().startsWith("name:")) {
                        return line.trim().removePrefix("name:").trim()
                    }
                }
            }

            val packageYaml = projectPath.resolve("package.yaml")

            if (Files.exists(packageYaml)) {
                Files.readAllLines(packageYaml).forEach { line ->
                    if (line.trim().startsWith("name:")) {
                        return line.trim().removePrefix("name:").trim()
                    }
                }
            }

            Files.list(projectPath).use { stream ->
                stream.filter { it.fileName.toString().endsWith(".cabal") }.findFirst().getOrNull()?.let { cabalFile ->
                    Files.readAllLines(cabalFile).forEach { line ->
                        if (line.trim().startsWith("name:")) {
                            return line.trim().removePrefix("name:").trim()
                        }
                    }
                }
            }

            return "unknown"
        }

        private fun parseResolver(projectPath: Path): String {
            val stackYaml = projectPath.resolve("stack.yaml")

            if (Files.exists(stackYaml)) {
                Files.readAllLines(stackYaml).forEach { line ->
                    if (line.trim().startsWith("resolver:")) {
                        return line.trim().removePrefix("resolver:").trim()
                    }
                }
            }

            throw StackException("No resolver found in stack.yaml")
        }

        private suspend fun getGhcVersion(projectPath: Path) = try {
            val process = ProcessBuilder(listOf("stack", "ghc", "--", "--version")).run {
                directory(projectPath.toFile())

                redirectErrorStream(true)

                start()
            }

            val output = process.inputStream.bufferedReader().use(Reader::readText)

            val exitCode = runInterruptible { process.waitFor() }

            if (exitCode != 0) {
                throw StackException("Failed to get GHC version: process exited with code $exitCode")
            }

            GHC_VERSION_PATTERN.find(output)?.groupValues?.get(1)
                ?: throw StackException("Could not parse GHC version from: $output")
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Throwable) {
            throw StackException("Failed to get GHC version: ${throwable.message}")
        }

        private fun parseDependencies(projectPath: Path): List<String> {
            val packageYaml = projectPath.resolve("package.yaml")

            if (!Files.exists(packageYaml)) return emptyList()

            val dependencies = mutableListOf<String>()

            var inDependencies = false

            Files.readAllLines(packageYaml).forEach { line ->
                val trimmed = line.trim()

                when {
                    trimmed == "dependencies:" -> inDependencies = true

                    inDependencies && trimmed.startsWith("-") -> dependencies.add(trimmed.removePrefix("-").trim())

                    inDependencies && trimmed.isNotEmpty() && !trimmed.startsWith("-") -> inDependencies = false
                }
            }

            return dependencies
        }

        private fun validateStackProject(projectPath: Path) {
            if (!Files.isDirectory(projectPath)) {
                throw StackException("Path is not a directory: $projectPath")
            }

            val stackYaml = projectPath.resolve("stack.yaml")

            if (!Files.exists(stackYaml)) {
                throw StackException("Not a stack project (no stack.yaml): $projectPath")
            }
        }

        override suspend fun getProject(path: String) = runCatching {
            val projectPath = Path.of(path)

            if (!Files.isDirectory(projectPath)) {
                throw StackException("Path is not a directory: $path")
            }

            val stackYaml = projectPath.resolve("stack.yaml")

            if (!Files.exists(stackYaml)) {
                throw StackException("No stack.yaml found in: $path")
            }

            val name = parseProjectName(projectPath)

            val resolver = parseResolver(projectPath)

            val ghcVersion = getGhcVersion(projectPath)

            val dependencies = parseDependencies(projectPath)

            StackProject(
                path = path, name = name, resolver = resolver, ghcVersion = ghcVersion, dependencies = dependencies
            )
        }

        override suspend fun create(path: String, name: String, template: String) = runCatching {
            val projectPath = Path.of(path)

            if (!Files.isDirectory(projectPath)) {
                throw StackException("Path is not a directory: $path")
            }

            executeStackCommand(
                workingDirectory = projectPath, args = arrayOf("new", name, template, "--bare"), operation = "create"
            )
        }

        override suspend fun build(path: String) = runCatching {
            val projectPath = Path.of(path)

            validateStackProject(projectPath)

            executeStackCommand(
                workingDirectory = projectPath, args = arrayOf("build"), operation = "build"
            )
        }

        override suspend fun run(path: String) = runCatching {
            val projectPath = Path.of(path)

            validateStackProject(projectPath)

            executeStackCommand(
                workingDirectory = projectPath, args = arrayOf("run"), operation = "run"
            )
        }

        override suspend fun test(path: String) = runCatching {
            val projectPath = Path.of(path)

            validateStackProject(projectPath)

            executeStackCommand(
                workingDirectory = projectPath, args = arrayOf("test"), operation = "test"
            )
        }

        override suspend fun clean(path: String) = runCatching {
            val projectPath = Path.of(path)

            validateStackProject(projectPath)

            executeStackCommand(
                workingDirectory = projectPath, args = arrayOf("clean"), operation = "clean"
            )
        }
    }
}