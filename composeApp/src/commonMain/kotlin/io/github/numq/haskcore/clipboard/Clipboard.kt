package io.github.numq.haskcore.clipboard

sealed interface Clipboard {
    data object Empty : Clipboard

    data class Cut(val paths: List<String>) : Clipboard

    data class Copy(val paths: List<String>) : Clipboard
}