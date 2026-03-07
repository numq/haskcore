package io.github.numq.haskcore.feature.explorer.core

import java.io.File

sealed interface ExplorerTree {
    val root: ExplorerRoot

    data class Loading(override val root: ExplorerRoot) : ExplorerTree

    data class Loaded(
        override val root: ExplorerRoot,
        val position: ExplorerPosition,
        val selectedPath: String,
        val nodes: List<ExplorerNode>
    ) : ExplorerTree {
        private fun createVirtualNode(path: String): ExplorerNode {
            val file = File(path)

            val level = path.removePrefix(root.path).count(File.separatorChar::equals)

            return when {
                file.isDirectory -> ExplorerNode.Directory(
                    name = file.name, path = path, level = level, isSelected = path == selectedPath
                )

                else -> ExplorerNode.File(
                    name = file.name, path = path, level = level, isSelected = path == selectedPath
                )
            }
        }

        val selection: SelectedExplorerNode? by lazy {
            when {
                selectedPath.isEmpty() -> null

                else -> {
                    val target = nodes.find { node ->
                        node.path == selectedPath
                    } ?: createVirtualNode(path = selectedPath)

                    val segments = generateSequence(selectedPath) { path ->
                        when (path) {
                            root.path -> null

                            else -> File(path).parent
                        }
                    }.map { path ->
                        nodes.find { node ->
                            node.path == path
                        } ?: createVirtualNode(path = path)
                    }.toList().reversed()

                    SelectedExplorerNode(node = target, segments = segments)
                }
            }
        }
    }
}