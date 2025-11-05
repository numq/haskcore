package io.github.numq.haskcore.buildsystem

import io.github.numq.haskcore.stack.StackComponent
import io.github.numq.haskcore.stack.StackOutput
import io.github.numq.haskcore.stack.StackPackage
import io.github.numq.haskcore.stack.StackProject
import io.github.numq.haskcore.timestamp.Timestamp

private fun StackComponent.toBuildSystemComponent(packageName: String) = when (this) {
    is StackComponent.Library -> BuildComponent.Library(
        path = path, name = name, packageName = packageName, exposedModules = exposedModules
    )

    is StackComponent.Executable -> BuildComponent.Executable(
        path = path, name = name, packageName = packageName, mainFile = mainFile
    )

    is StackComponent.Test -> BuildComponent.Test(
        path = path, name = name, packageName = packageName
    )

    is StackComponent.Benchmark -> BuildComponent.Benchmark(
        path = path, name = name, packageName = packageName
    )
}

private fun StackPackage.toBuildPackage() = BuildPackage(
    path = path,
    name = name,
    components = components.map { component -> component.toBuildSystemComponent(packageName = name) },
    buildSystem = BuildSystem.STACK,
    configFile = configFile
)

internal fun StackProject.toBuildProject() = BuildTarget.BuildProject.Stack(
    path = path,
    name = name,
    packages = packages.map(StackPackage::toBuildPackage),
    resolver = resolver,
    ghcVersion = ghcVersion
)

internal fun StackOutput.toBuildOutput(target: BuildTarget) = when (this) {
    is StackOutput.Progress -> BuildOutput.Progress(
        target = target, message = message, timestamp = Timestamp.now()
    )

    is StackOutput.BuildModule -> BuildOutput.BuildModule(
        module = module, target = target, message = message, timestamp = Timestamp.now()
    )

    is StackOutput.TestResult -> BuildOutput.TestResult(
        module = module, passed = passed, target = target, message = message, timestamp = Timestamp.now()
    )

    is StackOutput.Warning -> BuildOutput.Warning(
        target = target, message = message, timestamp = Timestamp.now()
    )

    is StackOutput.Error -> BuildOutput.Error(
        target = target, message = message, timestamp = Timestamp.now()
    )

    is StackOutput.RunOutput -> BuildOutput.Run(
        target = target, message = message, timestamp = Timestamp.now()
    )

    is StackOutput.Completion.Success -> BuildOutput.Completion.Success(
        exitCode = exitCode, duration = duration, target = target, timestamp = Timestamp.now()
    )

    is StackOutput.Completion.Failure -> BuildOutput.Completion.Failure(
        error = error, exitCode = exitCode, duration = duration, target = target, timestamp = Timestamp.now()
    )
}