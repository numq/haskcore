package io.github.numq.haskcore.explorer

import kotlinx.collections.immutable.*

internal data class ExplorerTree(
    val rootNode: ExplorerNode.Directory,
    val childrenByPath: PersistentMap<String, PersistentSet<ExplorerNode>> = persistentMapOf()
) {
    fun getExpandedPaths(): Set<String> {
        val expandedPaths = mutableSetOf<String>()

        fun collectExpandedPaths(node: ExplorerNode) {
            if (node is ExplorerNode.Directory && node.expanded) {
                expandedPaths.add(node.path)

                childrenByPath[node.path]?.forEach(::collectExpandedPaths)
            }
        }

        collectExpandedPaths(node = rootNode)

        return expandedPaths
    }

    fun applySnapshot(snapshot: ExplorerSnapshot): ExplorerTree {
        if (snapshot.expandedDirectoryPaths.isEmpty()) {
            return this
        }

        fun updateNodeWithSnapshot(node: ExplorerNode) = when (node) {
            is ExplorerNode.Directory -> node.copy(expanded = snapshot.expandedDirectoryPaths.contains(node.path))

            else -> node
        }

        val updatedRootNode = updateNodeWithSnapshot(rootNode) as? ExplorerNode.Directory ?: return this

        val updatedChildrenByPath = childrenByPath.mapValues { (_, nodes) ->
            nodes.map(::updateNodeWithSnapshot).toPersistentSet()
        }.toPersistentMap()

        return copy(rootNode = updatedRootNode, childrenByPath = updatedChildrenByPath)
    }

    fun flatten() = buildList {
        fun visit(node: ExplorerNode) {
            add(node)

            if (node is ExplorerNode.Directory && node.expanded) {
                childrenByPath[node.path].orEmpty().sortedWith(ExplorerNodeComparator).forEach(::visit)
            }
        }

        visit(node = rootNode)
    }
}