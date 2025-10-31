package io.github.numq.haskcore.buildsystem.exception

internal data object UnsupportedBuildSystemException : BuildSystemException(message = "Unsupported build system") {
    private fun readResolve(): Any = UnsupportedBuildSystemException
}