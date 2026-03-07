package io.github.numq.haskcore.feature.status.core

sealed interface StatusTool {
    data object Scanning : StatusTool

    data object NotFound : StatusTool

    data class Ready(val path: String, val version: String) : StatusTool

    data class Error(val throwable: Throwable) : StatusTool
}