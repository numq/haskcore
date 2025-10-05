package io.github.numq.haskcore.clipboard

internal sealed interface Clipboard {
    operator fun contains(path: String) = when (this) {
        is Empty -> false

        is Cut -> path in paths

        is Copy -> path in paths
    }

    data object Empty : Clipboard

    data class Cut(val paths: List<String>) : Clipboard

    data class Copy(val paths: List<String>) : Clipboard
}