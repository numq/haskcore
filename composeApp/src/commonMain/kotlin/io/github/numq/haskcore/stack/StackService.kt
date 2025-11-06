package io.github.numq.haskcore.stack

import io.github.numq.haskcore.command.ExecutedCommand
import io.github.numq.haskcore.output.OutputMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runInterruptible
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.milliseconds

internal interface StackService {
    suspend fun isAvailable(): Result<Boolean>

    suspend fun isStackProject(path: String): Result<Boolean>

    suspend fun getProject(path: String): Result<StackProject>

    suspend fun createProject(path: String, name: String, template: String?, bare: Boolean): Result<ExecutedCommand>

    suspend fun build(path: String): Result<ExecutedCommand>

    suspend fun run(path: String): Result<ExecutedCommand>

    suspend fun test(path: String): Result<ExecutedCommand>

    suspend fun clean(path: String): Result<ExecutedCommand>

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

            return StackOutput.TestResult(module, passed, line)
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
        ): ExecutedCommand {
            val args = listOf("stack", *args)

            val command = args.joinToString(" ")

            return ExecutedCommand(command = command, messages = callbackFlow {
                val startTime = System.currentTimeMillis()

                val process = ProcessBuilder(args).run {
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
                            exitCode = exitCode,
                            duration = duration,
                            error = "$operation failed with exit code $exitCode"
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
            }.map { stackOutput ->
                when (stackOutput) {
                    is StackOutput.Progress -> OutputMessage.Info(text = "📝 ${stackOutput.message}")

                    is StackOutput.Warning -> OutputMessage.Warning(text = "⚠️ ${stackOutput.message}")

                    is StackOutput.Error -> OutputMessage.Error(text = "❌ ${stackOutput.message}")

                    is StackOutput.BuildModule -> OutputMessage.Info(text = "🔨 [${stackOutput.module}] ${stackOutput.message}")

                    is StackOutput.RunOutput -> OutputMessage.Info(text = "🚀 ${stackOutput.message}")

                    is StackOutput.TestResult -> OutputMessage.Info(text = "🧪 [${stackOutput.module}] ${if (stackOutput.passed) "✅" else "❌"} ${stackOutput.message}")

                    is StackOutput.Completion.Success -> OutputMessage.Success(text = "✅ Completed successfully in ${stackOutput.duration}")

                    is StackOutput.Completion.Failure -> OutputMessage.Error(text = "💥 Failed with exit code ${stackOutput.exitCode}: ${stackOutput.error}")
                }
            }.flowOn(Dispatchers.IO))
        }

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
                stream.filter { path ->
                    path.fileName.absolutePathString().endsWith(".cabal")
                }.findFirst().getOrNull()?.let { cabalFile ->
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

        private fun parsePackages(projectPath: Path) = buildList {
            Files.list(projectPath).use { stream ->
                stream.filter(Files::isDirectory).forEach { dir ->
                    val cabalFiles = Files.list(dir).use { files ->
                        files.filter { path ->
                            path.fileName.absolutePathString().endsWith(".cabal")
                        }.toList()
                    }

                    if (cabalFiles.isNotEmpty()) {
                        val packageName = parseCabalPackageName(cabalFiles.first())

                        val components = parseCabalComponents(cabalFiles.first())

                        add(
                            StackPackage(
                                path = dir.absolutePathString(),
                                name = packageName,
                                components = components,
                                configFile = cabalFiles.first().absolutePathString()
                            )
                        )
                    }
                }
            }
        }

        private fun parseCabalPackageName(cabalFile: Path): String {
            Files.readAllLines(cabalFile).forEach { line ->
                if (line.trim().startsWith("name:")) {
                    return line.trim().removePrefix("name:").trim()
                }
            }

            return "unknown"
        }

        private fun parseCabalComponents(cabalFile: Path) = buildList {
            val lines = Files.readAllLines(cabalFile)

            for (line in lines) {
                val trimmed = line.trim()

                when {
                    trimmed.startsWith("library") -> {
                        val componentName = trimmed.removePrefix("library").trim().ifEmpty { "lib" }

                        add(
                            StackComponent.Library(
                                path = cabalFile.parent.resolve("src").absolutePathString(),
                                name = componentName,
                                exposedModules = emptyList()
                            )
                        )
                    }

                    trimmed.startsWith("executable") -> {
                        val componentName = trimmed.removePrefix("executable").trim()

                        add(
                            StackComponent.Executable(
                                path = cabalFile.parent.resolve("app").absolutePathString(),
                                name = componentName,
                                mainFile = null
                            )
                        )
                    }

                    trimmed.startsWith("test-suite") -> {
                        val componentName = trimmed.removePrefix("test-suite").trim()

                        add(
                            StackComponent.Test(
                                path = cabalFile.parent.resolve("test").absolutePathString(),
                                name = componentName,
                                mainFile = null
                            )
                        )
                    }

                    trimmed.startsWith("benchmark") -> {
                        val componentName = trimmed.removePrefix("benchmark").trim()

                        add(
                            StackComponent.Benchmark(
                                path = cabalFile.parent.resolve("bench").absolutePathString(),
                                name = componentName,
                                mainFile = null
                            )
                        )
                    }

                    else -> {
                        val lastIndex = lastIndex

                        if (lastIndex >= 0) {
                            val lastComponent = get(lastIndex)

                            when {
                                trimmed.startsWith("exposed-modules:") && lastComponent is StackComponent.Library -> {
                                    val exposedModules = trimmed.removePrefix("exposed-modules:").trim().split(' ')
                                        .filter(String::isNotBlank)

                                    set(lastIndex, lastComponent.copy(exposedModules = exposedModules))
                                }

                                trimmed.startsWith("main-is:") -> {
                                    val mainFile = trimmed.removePrefix("main-is:").trim()

                                    when (lastComponent) {
                                        is StackComponent.Executable -> set(
                                            lastIndex, lastComponent.copy(mainFile = mainFile)
                                        )

                                        is StackComponent.Test -> set(
                                            lastIndex, lastComponent.copy(mainFile = mainFile)
                                        )

                                        is StackComponent.Benchmark -> set(
                                            lastIndex, lastComponent.copy(mainFile = mainFile)
                                        )

                                        else -> Unit
                                    }
                                }
                            }
                        }
                    }
                }
            }
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

        override suspend fun isAvailable() = runCatching {
            val process = ProcessBuilder(listOf("stack", "--version")).start()

            val exitCode = runInterruptible { process.waitFor() }

            exitCode == 0
        }

        override suspend fun isStackProject(path: String) = runCatching {
            val projectPath = Path.of(path)

            Files.isDirectory(projectPath) && Files.exists(projectPath.resolve("stack.yaml"))
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

            val packages = parsePackages(projectPath)

            StackProject(
                path = path, name = name, packages = packages, resolver = resolver, ghcVersion = ghcVersion
            )
        }

        override suspend fun createProject(path: String, name: String, template: String?, bare: Boolean) = runCatching {
            val projectPath = Path.of(path)

            if (!Files.isDirectory(projectPath)) {
                throw StackException("Path is not a directory: $path")
            }

            val args = buildList {
                add("new")

                add(name)

                if (template != null) {
                    add(template)
                }

                if (bare) {
                    add("--bare")
                }
            }.toTypedArray()

            executeStackCommand(
                workingDirectory = projectPath, args = args, operation = "create"
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