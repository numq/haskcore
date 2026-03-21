package io.github.numq.haskcore.feature.explorer.core

@JvmInline
value class ExplorerRoot(val path: String) : Comparable<ExplorerRoot> {
    override fun compareTo(other: ExplorerRoot) = path.compareTo(other.path)
}