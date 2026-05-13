package io.github.numq.haskcore.feature.execution.core

internal fun LaunchTargetData.toLaunchTarget() = when (this) {
    is LaunchTargetData.Stack -> LaunchTarget.Stack(name = name, workingDir = workingDir, componentName = componentName)

    is LaunchTargetData.Cabal -> LaunchTarget.Cabal(name = name, workingDir = workingDir, componentName = componentName)

    is LaunchTargetData.File -> LaunchTarget.File(name = name, workingDir = workingDir, filePath = filePath)
}

internal fun LaunchTarget.toLaunchTargetData() = when (this) {
    is LaunchTarget.Stack -> LaunchTargetData.Stack(name = name, workingDir = workingDir, componentName = componentName)

    is LaunchTarget.Cabal -> LaunchTargetData.Cabal(name = name, workingDir = workingDir, componentName = componentName)

    is LaunchTarget.File -> LaunchTargetData.File(name = name, workingDir = workingDir, filePath = filePath)
}

internal fun BeforeRunTaskData.toBeforeRunTask() = when (this) {
    is BeforeRunTaskData.Build -> BeforeRunTask.Build(id = id, isEnabled = isEnabled, cleanFirst = cleanFirst)

    is BeforeRunTaskData.ExternalTool -> BeforeRunTask.ExternalTool(
        id = id, isEnabled = isEnabled, command = command, arguments = arguments, workingDir = workingDir
    )
}

internal fun BeforeRunTask.toBeforeRunTaskData() = when (this) {
    is BeforeRunTask.Build -> BeforeRunTaskData.Build(id = id, isEnabled = isEnabled, cleanFirst = cleanFirst)

    is BeforeRunTask.ExternalTool -> BeforeRunTaskData.ExternalTool(
        id = id, isEnabled = isEnabled, command = command, arguments = arguments, workingDir = workingDir
    )
}

internal fun ExecutionConfigurationData.toExecutionConfiguration() = ExecutionConfiguration(
    id = id,
    name = name,
    target = target.toLaunchTarget(),
    programArguments = programArguments,
    env = env,
    beforeRun = beforeRun.map(BeforeRunTaskData::toBeforeRunTask),
)

internal fun ExecutionConfiguration.toExecutionConfigurationData() = ExecutionConfigurationData(
    id = id,
    name = name,
    target = target.toLaunchTargetData(),
    programArguments = programArguments,
    env = env,
    beforeRun = beforeRun.map(BeforeRunTask::toBeforeRunTaskData),
)