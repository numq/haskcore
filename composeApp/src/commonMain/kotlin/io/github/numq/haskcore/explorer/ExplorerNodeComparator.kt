package io.github.numq.haskcore.explorer

object ExplorerNodeComparator : Comparator<ExplorerNode> {
    override fun compare(a: ExplorerNode, b: ExplorerNode): Int {
        val aIsDir = a is ExplorerNode.Directory

        val bIsDir = b is ExplorerNode.Directory

        if (aIsDir != bIsDir) {
            return if (aIsDir) -1 else 1
        }

        return a.name.compareTo(b.name, ignoreCase = true)
    }
}