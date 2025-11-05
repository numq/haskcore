package io.github.numq.haskcore.buildsystem

internal enum class BuildProjectOperation(val displayName: String) {
    BUILD("Build"), RUN("Run"), TEST("Test"), CLEAN("Clean")
}