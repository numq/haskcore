package io.github.numq.haskcore.explorer

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf

internal data class ExplorerTree(
    val root: ExplorerNode.Directory,
    val childrenByPath: PersistentMap<String, PersistentSet<ExplorerNode>> = persistentMapOf()
)