package io.github.numq.haskcore.buildsystem

import io.github.numq.haskcore.stack.StackComponent
import io.github.numq.haskcore.stack.StackOutput
import io.github.numq.haskcore.stack.StackPackage
import io.github.numq.haskcore.stack.StackProject
import io.github.numq.haskcore.timestamp.Timestamp

internal fun StackProject.toBuildProject() = BuildSystemArtifact.BuildProject.Stack(
    path = path,
    name = name,
    packages = packages.map(StackPackage::toBuildPackage),
    resolver = resolver,
    ghcVersion = ghcVersion
)

private fun StackPackage.toBuildPackage() = BuildSystemArtifact.BuildPackage(
    path = path,
    name = name,
    components = components.map { component -> component.toBuildComponent(packageName = name) },
    buildSystem = BuildSystem.STACK,
    configFile = configFile
)

private fun StackComponent.toBuildComponent(packageName: String) = when (this) {
    is StackComponent.Library -> BuildSystemArtifact.BuildComponent.Library(
        path = path, name = name, packageName = packageName, exposedModules = exposedModules
    )

    is StackComponent.Executable -> BuildSystemArtifact.BuildComponent.Executable(
        path = path, name = name, packageName = packageName, mainFile = mainFile
    )

    is StackComponent.Test -> BuildSystemArtifact.BuildComponent.Test(
        path = path, name = name, packageName = packageName
    )

    is StackComponent.Benchmark -> BuildSystemArtifact.BuildComponent.Benchmark(
        path = path, name = name, packageName = packageName
    )
}

internal fun StackOutput.toBuildOutput(): BuildOutput = when (this) {
    is StackOutput.Progress -> BuildOutput.ProgressOutput(
        message = message, timestamp = Timestamp.now()
    )

    is StackOutput.Warning -> BuildOutput.WarningOutput(
        message = message, timestamp = Timestamp.now()
    )

    is StackOutput.Error -> BuildOutput.ErrorOutput(
        message = message, timestamp = Timestamp.now()
    )

    is StackOutput.BuildModule -> BuildOutput.BuildModuleOutput(
        module = module, message = message, timestamp = Timestamp.now()
    )

    is StackOutput.RunOutput -> BuildOutput.RunOutput(
        message = message, timestamp = Timestamp.now()
    )

    is StackOutput.TestResult -> BuildOutput.TestResultOutput(
        module = module, passed = passed, message = message, timestamp = Timestamp.now()
    )

    is StackOutput.Completion.Success -> BuildOutput.CompletionOutput.Success(
        exitCode = exitCode, duration = duration, timestamp = Timestamp.now()
    )

    is StackOutput.Completion.Failure -> BuildOutput.CompletionOutput.Failure(
        exitCode = exitCode, duration = duration, error = error, timestamp = Timestamp.now()
    )
}